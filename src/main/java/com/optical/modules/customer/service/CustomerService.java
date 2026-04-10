package com.optical.modules.customer.service;

import com.optical.common.base.PageResponse;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.common.enums.ChequeStatus;
import com.optical.modules.billing.entity.CustomerBill;
import com.optical.modules.billing.entity.CustomerBillPayment;
import com.optical.modules.billing.repository.CustomerBillPaymentRepository;
import com.optical.modules.billing.repository.CustomerBillRepository;
import com.optical.modules.customer.dto.CustomerPageResponse;
import com.optical.modules.customer.dto.CustomerChequeStatusUpdateRequest;
import com.optical.modules.customer.dto.CustomerOpeningBalancePaymentRequest;
import com.optical.modules.customer.dto.CustomerOpeningBalancePaymentResponse;
import com.optical.modules.customer.dto.CustomerPendingBillResponse;
import com.optical.modules.customer.dto.CustomerPendingBillsResponse;
import com.optical.modules.customer.dto.CustomerPendingPaymentAllocationRequest;
import com.optical.modules.customer.dto.CustomerPendingPaymentAllocationResponse;
import com.optical.modules.customer.dto.CustomerPendingPaymentRequest;
import com.optical.modules.customer.dto.CustomerPendingPaymentResponse;
import com.optical.modules.customer.dto.CustomerReceivedChequeResponse;
import com.optical.modules.customer.dto.CustomerRequest;
import com.optical.modules.customer.dto.CustomerResponse;
import com.optical.modules.customer.dto.CustomerSummaryResponse;
import com.optical.modules.customer.entity.Customer;
import com.optical.modules.customer.entity.CustomerCreditLedger;
import com.optical.modules.customer.repository.CustomerCreditLedgerRepository;
import com.optical.modules.customer.repository.CustomerRepository;
import com.optical.modules.patient.repository.PatientRepository;
import com.optical.modules.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerBillRepository customerBillRepository;
    private final CustomerBillPaymentRepository customerBillPaymentRepository;
    private final CustomerCreditLedgerRepository customerCreditLedgerRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;

    public CustomerResponse create(CustomerRequest request) {
        String normalizedPhone = normalize(request.getPhone());
        String normalizedEmail = normalize(request.getEmail());

        if (normalizedPhone != null
                && customerRepository.existsByPhoneAndDeletedAtIsNull(normalizedPhone)) {
            throw new RuntimeException("Customer phone already exists");
        }
        if (normalizedEmail != null
                && customerRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new RuntimeException("Customer email already exists");
        }

        Customer customer = new Customer();
        applyRequest(customer, request);
        Customer saved = customerRepository.save(customer);
        recordOpeningBalance(saved);
        return mapToResponse(saved);
    }

    public List<CustomerResponse> getAll() {
        return customerRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CustomerPageResponse search(String q, int page, int size) {
        Page<Customer> result = customerRepository.search(q, PageRequest.of(page, size));
        List<CustomerResponse> items = result.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return CustomerPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    public CustomerResponse getById(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerSummaryResponse getSummary(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return CustomerSummaryResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .pendingAmount(zeroIfNull(customer.getPendingAmount()))
                .totalBills(customerBillRepository.countActiveByCustomerId(id))
                .totalBilledAmount(zeroIfNull(customerBillRepository.sumTotalAmountByCustomerId(id)))
                .totalPaidAmount(zeroIfNull(customerBillRepository.sumPaidAmountByCustomerId(id)))
                .totalOutstandingAmount(zeroIfNull(customerBillRepository.sumBalanceAmountByCustomerId(id)))
                .totalPatients(patientRepository.countActiveByCustomerId(id))
                .totalPrescriptions(prescriptionRepository.countActiveByCustomerId(id))
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerPendingBillsResponse getPendingBills(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<CustomerPendingBillResponse> bills = customerBillRepository.findPendingBillsByCustomerId(id).stream()
                .map(this::mapPendingBill)
                .toList();

        return CustomerPendingBillsResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .totalPendingAmount(zeroIfNull(customer.getPendingAmount()))
                .customerBills(bills)
                .build();
    }

    @Transactional
    public CustomerPendingPaymentResponse payPendingBills(Long id, CustomerPendingPaymentRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (request.getPaymentMode() == com.optical.modules.purchase.entity.PaymentMode.CREDIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer payment mode cannot be CREDIT");
        }
        if (request.getPaymentMode() == com.optical.modules.purchase.entity.PaymentMode.CHEQUE) {
            validateChequeDetails(request);
        }

        BigDecimal amount = scale(request.getAmount());
        BigDecimal currentPendingAmount = scale(zeroIfNull(customer.getPendingAmount()));
        if (amount.compareTo(currentPendingAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount cannot exceed customer pending amount");
        }

        Map<Long, CustomerPendingPaymentAllocationRequest> allocationRequests = new LinkedHashMap<>();
        for (CustomerPendingPaymentAllocationRequest allocation : request.getAllocations()) {
            if (allocationRequests.putIfAbsent(allocation.getBillId(), allocation) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate billId allocations are not allowed");
            }
        }

        BigDecimal allocatedAmount = allocationRequests.values().stream()
                .map(CustomerPendingPaymentAllocationRequest::getAmount)
                .map(this::scale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocatedAmount.compareTo(amount) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocated amount must exactly match payment amount");
        }

        List<CustomerBill> bills = customerBillRepository.findActiveByCustomerIdAndIdIn(id, List.copyOf(allocationRequests.keySet()));
        if (bills.size() != allocationRequests.size()) {
            throw new ResourceNotFoundException("One or more customer bills were not found");
        }

        Map<Long, CustomerBill> billsById = new LinkedHashMap<>();
        for (CustomerBill bill : bills) {
            billsById.put(bill.getId(), bill);
        }

        List<CustomerBillPayment> payments = allocationRequests.entrySet().stream()
                .map(entry -> buildPayment(entry.getValue(), billsById.get(entry.getKey()), request))
                .toList();
        customerBillPaymentRepository.saveAll(payments);

        customer.setPendingAmount(scale(currentPendingAmount.subtract(amount)));

        CustomerCreditLedger ledger = new CustomerCreditLedger();
        ledger.setCustomer(customer);
        ledger.setAmount(scale(amount.negate()));
        ledger.setEntryType("PAYMENT");
        ledger.setReference(resolvePaymentReference(request.getReference()));
        customerCreditLedgerRepository.save(ledger);

        List<CustomerPendingPaymentAllocationResponse> allocationResponses = payments.stream()
                .map(this::mapAllocationResponse)
                .toList();

        return CustomerPendingPaymentResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .paymentMode(request.getPaymentMode())
                .amount(amount)
                .reference(normalize(request.getReference()))
                .totalPendingAmount(customer.getPendingAmount())
                .allocations(allocationResponses)
                .build();
    }

    @Transactional
    public CustomerOpeningBalancePaymentResponse payOpeningBalance(Long id, CustomerOpeningBalancePaymentRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (request.getPaymentMode() == com.optical.modules.purchase.entity.PaymentMode.CREDIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer payment mode cannot be CREDIT");
        }
        if (request.getPaymentMode() == com.optical.modules.purchase.entity.PaymentMode.CHEQUE) {
            validateChequeDetails(
                    request.getChequeNumber(),
                    request.getChequeDate(),
                    request.getChequeBankName()
            );
        }

        BigDecimal amount = scale(request.getAmount());
        BigDecimal currentPendingAmount = scale(zeroIfNull(customer.getPendingAmount()));
        if (amount.compareTo(currentPendingAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount cannot exceed customer pending amount");
        }

        customer.setPendingAmount(scale(currentPendingAmount.subtract(amount)));

        CustomerCreditLedger ledger = new CustomerCreditLedger();
        ledger.setCustomer(customer);
        ledger.setAmount(scale(amount.negate()));
        ledger.setEntryType("OPENING_PAYMENT");
        ledger.setReference(resolvePaymentReference(request.getReference()));
        customerCreditLedgerRepository.save(ledger);

        return CustomerOpeningBalancePaymentResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .paymentMode(request.getPaymentMode())
                .amount(amount)
                .reference(normalize(request.getReference()))
                .totalPendingAmount(customer.getPendingAmount())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerReceivedChequeResponse> getReceivedCheques(Long customerId, ChequeStatus status, int page, int size) {
        Page<CustomerBillPayment> result = customerBillPaymentRepository.findReceivedCheques(
                customerId,
                status,
                PageRequest.of(page, size)
        );

        return PageResponse.<CustomerReceivedChequeResponse>builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .content(result.getContent().stream().map(this::mapReceivedCheque).toList())
                .build();
    }

    @Transactional
    public CustomerReceivedChequeResponse updateReceivedChequeStatus(Long paymentId, CustomerChequeStatusUpdateRequest request) {
        CustomerBillPayment payment = customerBillPaymentRepository.findActiveByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer cheque payment not found"));

        if (payment.getPaymentMode() != com.optical.modules.purchase.entity.PaymentMode.CHEQUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not a cheque payment");
        }

        ChequeStatus currentStatus = payment.getChequeStatus();
        if (currentStatus != request.getExpectedCurrentStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cheque status has changed. Refresh and try again");
        }

        ChequeStatus newStatus = request.getNewStatus();
        if (currentStatus != newStatus) {
            boolean currentApplied = isFinanciallyApplied(currentStatus);
            boolean nextApplied = isFinanciallyApplied(newStatus);
            if (currentApplied && !nextApplied) {
                reverseReceivedCheque(payment);
            } else if (!currentApplied && nextApplied) {
                applyReceivedCheque(payment);
            }
        }

        payment.setChequeStatus(newStatus);
        if (newStatus == ChequeStatus.CLEARED) {
            payment.setChequeSettlementMode(request.getSettlementMode());
        }
        payment.setChequeStatusNotes(normalize(request.getNotes()));
        payment.setChequeStatusChangedAt(LocalDateTime.now());
        return mapReceivedCheque(payment);
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String normalizedPhone = normalize(request.getPhone());
        String normalizedEmail = normalize(request.getEmail());

        if (normalizedPhone != null
                && customerRepository.existsByPhoneAndDeletedAtIsNullAndIdNot(normalizedPhone, id)) {
            throw new RuntimeException("Customer phone already exists");
        }
        if (normalizedEmail != null
                && customerRepository.existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(normalizedEmail, id)) {
            throw new RuntimeException("Customer email already exists");
        }

        applyRequest(customer, request);
        Customer saved = customerRepository.save(customer);
        return mapToResponse(saved);
    }

    public void delete(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.getName());
        customer.setPhone(normalize(request.getPhone()));
        customer.setEmail(normalize(request.getEmail()));
        customer.setAddress(request.getAddress());
        customer.setGender(request.getGender());
        customer.setDob(request.getDob());
        customer.setNotes(request.getNotes());
        customer.setPendingAmount(resolvePendingAmount(request.getPendingAmount()));
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .gender(customer.getGender())
                .dob(customer.getDob())
                .notes(customer.getNotes())
                .pendingAmount(customer.getPendingAmount())
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private CustomerPendingBillResponse mapPendingBill(CustomerBill bill) {
        return CustomerPendingBillResponse.builder()
                .billId(bill.getId())
                .billNumber(bill.getBillNumber())
                .billDate(bill.getBillDate())
                .totalAmount(zeroIfNull(bill.getTotalAmount()))
                .paidAmount(zeroIfNull(bill.getPaidAmount()))
                .pendingAmount(zeroIfNull(bill.getBalanceAmount()))
                .currencyCode(bill.getCurrencyCode())
                .build();
    }

    private CustomerBillPayment buildPayment(
            CustomerPendingPaymentAllocationRequest allocation,
            CustomerBill bill,
            CustomerPendingPaymentRequest request
    ) {
        if (bill == null) {
            throw new ResourceNotFoundException("Customer bill not found");
        }

        BigDecimal allocationAmount = scale(allocation.getAmount());
        BigDecimal balanceAmount = scale(zeroIfNull(bill.getBalanceAmount()));
        if (allocationAmount.compareTo(balanceAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocation exceeds pending amount for bill: " + bill.getId());
        }

        bill.setPaidAmount(scale(zeroIfNull(bill.getPaidAmount()).add(allocationAmount)));
        bill.setBalanceAmount(scale(balanceAmount.subtract(allocationAmount)));

        CustomerBillPayment payment = new CustomerBillPayment();
        payment.setCustomerBill(bill);
        payment.setPaymentMode(request.getPaymentMode());
        payment.setAmount(allocationAmount);
        payment.setChequeNumber(normalize(request.getChequeNumber()));
        payment.setChequeDate(request.getChequeDate());
        payment.setChequeBankName(normalize(request.getChequeBankName()));
        payment.setChequeBranchName(normalize(request.getChequeBranchName()));
        payment.setChequeAccountHolder(normalize(request.getChequeAccountHolder()));
        payment.setReference(normalize(request.getReference()));
        if (request.getPaymentMode() == com.optical.modules.purchase.entity.PaymentMode.CHEQUE) {
            payment.setChequeStatus(ChequeStatus.PENDING);
            payment.setChequeStatusChangedAt(LocalDateTime.now());
        }
        return payment;
    }

    private CustomerPendingPaymentAllocationResponse mapAllocationResponse(CustomerBillPayment payment) {
        CustomerBill bill = payment.getCustomerBill();
        return CustomerPendingPaymentAllocationResponse.builder()
                .billId(bill.getId())
                .billNumber(bill.getBillNumber() == null ? "BILL-" + bill.getId() : bill.getBillNumber())
                .paidAmount(payment.getAmount())
                .remainingPendingAmount(scale(zeroIfNull(bill.getBalanceAmount())))
                .build();
    }

    private void validateChequeDetails(CustomerPendingPaymentRequest request) {
        validateChequeDetails(request.getChequeNumber(), request.getChequeDate(), request.getChequeBankName());
    }

    private void validateChequeDetails(String chequeNumber, java.time.LocalDate chequeDate, String chequeBankName) {
        if (normalize(chequeNumber) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeNumber is required for cheque payments");
        }
        if (chequeDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeDate is required for cheque payments");
        }
        if (normalize(chequeBankName) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeBankName is required for cheque payments");
        }
    }

    private String resolvePaymentReference(String reference) {
        String normalizedReference = normalize(reference);
        return normalizedReference == null ? "CUSTOMER-PAYMENT" : normalizedReference;
    }

    private BigDecimal resolvePendingAmount(BigDecimal pendingAmount) {
        BigDecimal resolved = pendingAmount == null ? BigDecimal.ZERO : scale(pendingAmount);
        if (resolved.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pendingAmount cannot be negative");
        }
        return resolved;
    }

    private void recordOpeningBalance(Customer customer) {
        BigDecimal pendingAmount = scale(zeroIfNull(customer.getPendingAmount()));
        if (pendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CustomerCreditLedger ledger = new CustomerCreditLedger();
        ledger.setCustomer(customer);
        ledger.setAmount(pendingAmount);
        ledger.setEntryType("OPENING_BALANCE");
        ledger.setReference("CUSTOMER-OPENING-BALANCE");
        customerCreditLedgerRepository.save(ledger);
    }

    private CustomerReceivedChequeResponse mapReceivedCheque(CustomerBillPayment payment) {
        CustomerBill bill = payment.getCustomerBill();
        Customer customer = bill.getCustomer();
        return CustomerReceivedChequeResponse.builder()
                .paymentId(payment.getId())
                .customerId(customer == null ? null : customer.getId())
                .customerName(customer == null ? null : customer.getName())
                .billId(bill.getId())
                .billNumber(bill.getBillNumber() == null ? "BILL-" + bill.getId() : bill.getBillNumber())
                .billDate(bill.getBillDate())
                .amount(payment.getAmount())
                .chequeStatus(payment.getChequeStatus())
                .chequeNumber(payment.getChequeNumber())
                .chequeDate(payment.getChequeDate())
                .chequeBankName(payment.getChequeBankName())
                .chequeBranchName(payment.getChequeBranchName())
                .chequeAccountHolder(payment.getChequeAccountHolder())
                .settlementMode(payment.getChequeSettlementMode())
                .reference(payment.getReference())
                .statusNotes(payment.getChequeStatusNotes())
                .statusChangedAt(payment.getChequeStatusChangedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private boolean isFinanciallyApplied(ChequeStatus status) {
        return status == ChequeStatus.PENDING || status == ChequeStatus.CLEARED;
    }

    private void reverseReceivedCheque(CustomerBillPayment payment) {
        CustomerBill bill = payment.getCustomerBill();
        Customer customer = bill.getCustomer();
        BigDecimal amount = scale(payment.getAmount());
        BigDecimal paidAmount = scale(zeroIfNull(bill.getPaidAmount()));

        if (amount.compareTo(paidAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill paid amount is too low to reject this cheque");
        }

        bill.setPaidAmount(scale(paidAmount.subtract(amount)));
        bill.setBalanceAmount(scale(zeroIfNull(bill.getBalanceAmount()).add(amount)));
        customer.setPendingAmount(scale(zeroIfNull(customer.getPendingAmount()).add(amount)));

        CustomerCreditLedger ledger = new CustomerCreditLedger();
        ledger.setCustomer(customer);
        ledger.setAmount(amount);
        ledger.setEntryType("ADJUSTMENT");
        ledger.setReference("CHEQUE-REJECTED:" + payment.getId());
        customerCreditLedgerRepository.save(ledger);
    }

    private void applyReceivedCheque(CustomerBillPayment payment) {
        CustomerBill bill = payment.getCustomerBill();
        Customer customer = bill.getCustomer();
        BigDecimal amount = scale(payment.getAmount());
        BigDecimal balanceAmount = scale(zeroIfNull(bill.getBalanceAmount()));
        BigDecimal customerPendingAmount = scale(zeroIfNull(customer.getPendingAmount()));

        if (amount.compareTo(balanceAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill pending amount is too low to re-apply this cheque");
        }
        if (amount.compareTo(customerPendingAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer pending amount is too low to re-apply this cheque");
        }

        bill.setPaidAmount(scale(zeroIfNull(bill.getPaidAmount()).add(amount)));
        bill.setBalanceAmount(scale(balanceAmount.subtract(amount)));
        customer.setPendingAmount(scale(customerPendingAmount.subtract(amount)));

        CustomerCreditLedger ledger = new CustomerCreditLedger();
        ledger.setCustomer(customer);
        ledger.setAmount(scale(amount.negate()));
        ledger.setEntryType("ADJUSTMENT");
        ledger.setReference("CHEQUE-REAPPLIED:" + payment.getId());
        customerCreditLedgerRepository.save(ledger);
    }
}
