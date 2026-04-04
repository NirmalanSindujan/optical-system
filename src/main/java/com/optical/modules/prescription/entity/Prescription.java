package com.optical.modules.prescription.entity;

import com.optical.common.base.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.optical.modules.billing.entity.CustomerBill;
import com.optical.modules.patient.entity.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "prescription")
@Getter
@Setter
public class Prescription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_bill_id", unique = true)
    private CustomerBill customerBill;

    @Column(name = "prescription_date", nullable = false)
    private LocalDate prescriptionDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "values", nullable = false, columnDefinition = "jsonb")
    private JsonNode values;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
