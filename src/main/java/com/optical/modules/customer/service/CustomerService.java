package com.optical.modules.customer.service;

import com.optical.modules.customer.dto.CustomerPageResponse;
import com.optical.modules.customer.dto.CustomerRequest;
import com.optical.modules.customer.dto.CustomerResponse;
import com.optical.modules.customer.dto.CustomerSummaryResponse;
import com.optical.modules.customer.entity.Customer;
import com.optical.modules.customer.repository.CustomerRepository;
import com.optical.modules.billing.repository.CustomerBillRepository;
import com.optical.modules.patient.repository.PatientRepository;
import com.optical.modules.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerBillRepository customerBillRepository;
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
}
