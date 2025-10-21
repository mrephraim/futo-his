package com.example.data.models

import com.example.data.database.*
import kotlinx.datetime.Clock
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone


@Serializable
data class UnitOptionResponse(
    val id: Int,
    val name: String,
    val type: String
)

@Serializable
data class DrugDosageUnitOption(
    val id: Int,
    val name: String,
    val type: String // "base_package" or "base_unit"
)

@Serializable
data class PatientLookupRequest(
    val regNo: String
)

@Serializable
data class PatientPharmacyData(
    val regNo: String,
    val fullName: String,
    val dob: String?,
    val sex: String,
    val school: String?,
    val department: String?,
    val phoneNo: String?,
    val email: String?,
    val weight: Double?,
    val height: Double?,
    val bloodGroup: String?,
    val genotype: String?,
    val allergies: String?,        // pulled from previousIllness or dedicated field if you have one
    val previousIllness: String?
)


fun getDrugBasePackageAndUnits(drugId: Int): List<DrugDosageUnitOption> {
    return transaction {
        val basePackages = DrugPackages
            .selectAll()
            .where { (DrugPackages.drugId eq drugId) and (DrugPackages.subPackageId.isNull()) }
            .toList()

        val unitOptions = mutableListOf<DrugDosageUnitOption>()

        for (pkg in basePackages) {
            val pkgId = pkg[DrugPackages.id].value
            val formId = pkg[DrugPackages.formId]
            val strengthValue = pkg[DrugPackages.strengthValue]
            val strengthUnitId = pkg[DrugPackages.strengthUnitId]

            val formName = DosageForms
                .selectAll()
                .where { DosageForms.id eq formId.value }
                .single()[DosageForms.name]

            strengthUnitId?.let { suid ->
                val unit = Units
                    .selectAll()
                    .where { Units.id eq suid.value }
                    .single()
                val unitName = unit[Units.name].lowercase(Locale.getDefault())
                unitOptions.add(
                    DrugDosageUnitOption(
                        id = pkgId,
                        name = "$strengthValue$unitName $formName",
                        type = "base_package"
                    )
                )

            }



        }

        unitOptions.distinctBy { it.name }
    }
}

fun fetchPatientPharmacyData(regNo: String): PatientPharmacyData? {
    return transaction {
        // Basic patient info
        val patientRow = EmrPatients
            .selectAll()
            .where { EmrPatients.regNo eq regNo }
            .singleOrNull() ?: return@transaction null

        val fullName = listOfNotNull(
            patientRow[EmrPatients.surName],
            patientRow[EmrPatients.firstName],
            patientRow[EmrPatients.middleName]
        ).joinToString(" ")

        // Medical history
        val historyRow = EmrPatientMedicalHistory
            .selectAll()
            .where { EmrPatientMedicalHistory.regNo eq regNo }
            .singleOrNull()

        PatientPharmacyData(
            regNo = patientRow[EmrPatients.regNo],
            fullName = fullName,
            dob = patientRow[EmrPatients.dob].toString(),
            sex = patientRow[EmrPatients.sex],
            school = patientRow[EmrPatients.school],
            department = patientRow[EmrPatients.department],
            phoneNo = patientRow[EmrPatients.phoneNo],
            email = patientRow[EmrPatients.email],
            weight = historyRow?.getOrNull(EmrPatientMedicalHistory.weight)?.toDouble(),
            height = historyRow?.getOrNull(EmrPatientMedicalHistory.height)?.toDouble(),
            bloodGroup = historyRow?.getOrNull(EmrPatientMedicalHistory.bloodGroup),
            genotype = historyRow?.getOrNull(EmrPatientMedicalHistory.genotype),
            allergies = historyRow?.getOrNull(EmrPatientMedicalHistory.familyHistory), // or dedicated allergies field
            previousIllness = historyRow?.getOrNull(EmrPatientMedicalHistory.previousIllness)
        )
    }
}

@Serializable
data class CreatePrescriptionRequest(
    val patientId: String,
    val prescriberId: Int,
    val appointmentId: Int? = null,
    val prescriberDept: PrescriberDept,
    val medications: List<CreateMedicationRequest>
)

@Serializable
data class CreateMedicationRequest(
    val drugId: Int,
    val dosageQuantity: Double,
    val packageId: Int,
    val intakeInterval: Int,
    val durationValue: Int,
    val durationUnit: Int,
    val instruction: String? = null
)

@Serializable
data class CreatePrescriptionResponse(
    val success: Boolean,
    val message: String,
    val prescriptionId: Int? = null
)



fun createPrescription(request: CreatePrescriptionRequest): Int {
    return transaction {
        // Insert main prescription
        val newPrescriptionId = Prescriptions.insertAndGetId { row ->
            row[patientId] = request.patientId
            row[prescriberId] = request.prescriberId
            row[appointmentId] = request.appointmentId
            row[prescriberDept] = request.prescriberDept
            row[status] = PrescriptionStatus.PRESCRIBED
        }

        // Insert all medications
        request.medications.forEach { med ->
            PrescriptionMedications.insertAndGetId { row ->
                row[prescriptionId] = newPrescriptionId
                row[drugId] = med.drugId
                row[dosageQuantity] = med.dosageQuantity
                row[packageId] = med.packageId
                row[intakeInterval] = med.intakeInterval
                row[durationValue] = med.durationValue
                row[durationUnit] = med.durationUnit
                row[instruction] = med.instruction
            }
        }

        newPrescriptionId.value // return generated prescription id
    }
}


fun dispensePrescription(prescriptionId: Int) {
    transaction {
        // Step 1: Get all medications for this prescription
        val medications = PrescriptionMedications
            .selectAll().where { PrescriptionMedications.prescriptionId eq prescriptionId }
            .map {
                MedicationOrder(
                    drugId = it[PrescriptionMedications.drugId],
                    packageId = it[PrescriptionMedications.packageId],
                    requiredQty = (BigDecimal.valueOf(it[PrescriptionMedications.dosageQuantity]) *
                            BigDecimal(it[PrescriptionMedications.intakeInterval]) *
                            BigDecimal(it[PrescriptionMedications.durationValue]) *
                            BigDecimal(it[PrescriptionMedications.durationUnit])
                            )
                )
            }

        // Step 2: Check availability for each medication
        for (med in medications) {
            if (!isStockAvailable(med.drugId, med.packageId, med.requiredQty)) {
                error("Can't dispense drug: Drug ${med.drugId}, Package ${med.packageId} not available")
            }
        }

        // Step 3: Deduct stock batch by batch (FEFO)
        for (med in medications) {
            var qtyToDispense = med.requiredQty

            // Get eligible batches: not expired, same drug+package, order by expiry then received date
            val now: LocalDate = Clock.System.todayIn(TimeZone.UTC)

            val batches = InventoryBatches
                .selectAll().where{
                    (InventoryBatches.drugId eq med.drugId) and
                            (InventoryBatches.packageId eq med.packageId) and
                            (InventoryBatches.remainingUom greater BigDecimal.ZERO) and
                            ((InventoryBatches.expiryDate greaterEq now) or (InventoryBatches.expiryDate.isNull()))
                }
                .orderBy(InventoryBatches.expiryDate to SortOrder.ASC, InventoryBatches.receivedAt to SortOrder.ASC)

            for (batch in batches) {
                if (qtyToDispense <= BigDecimal.ZERO) break

                val batchId = batch[InventoryBatches.id].value
                val remaining = batch[InventoryBatches.remainingUom]

                val deduct = if (remaining >= qtyToDispense) qtyToDispense else remaining

                // Update batch remaining
                InventoryBatches.update({ InventoryBatches.id eq batchId }) {
                    it[remainingUom] = remaining - deduct
                }

                qtyToDispense -= deduct
            }

            // If still not fully dispensed, something went wrong (concurrency case)
            if (qtyToDispense > BigDecimal.ZERO) {
                error("Unexpected error: stock ran out during dispensing for drug ${med.drugId}")
            }
        }

        // Step 4: Update prescription status
        Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
            it[status] = PrescriptionStatus.ONGOING
        }
    }
}

data class MedicationOrder(
    val drugId: Int,
    val packageId: Int,
    val requiredQty: BigDecimal
)
@Serializable
data class PrescriberDetails(
    val name: String,
    val dept: PrescriberDept
)

fun getPrescriberDetails(prescriptionId: Int): PrescriberDetails? {
    return transaction {
        // 1. Get prescriber info from prescription
        val prescription = Prescriptions
            .selectAll().where { Prescriptions.id eq prescriptionId }
            .singleOrNull() ?: return@transaction null

        val prescriberId = prescription[Prescriptions.prescriberId]
        when (val dept = prescription[Prescriptions.prescriberDept]) {
            PrescriberDept.EMR -> {
                // Try fetching doctor
                val doctor = EmrDoctors
                    .selectAll().where { EmrDoctors.doctorId eq prescriberId.toString() }
                    .singleOrNull()

                if (doctor != null) {
                    val name = doctor[EmrDoctors.name]
                        .takeIf { it.isNotBlank() }
                        ?: EmrUsers
                            .selectAll().where { EmrUsers.id eq prescriberId }
                            .singleOrNull()
                            ?.get(EmrUsers.username)
                        ?: "Unknown"

                    PrescriberDetails(name, dept)
                } else {
                    // fallback if no doctor record found
                    val user = EmrUsers
                        .selectAll().where { EmrUsers.id eq prescriberId }
                        .singleOrNull()

                    val name = user?.get(EmrUsers.username) ?: "Unknown"
                    PrescriberDetails(name, dept)
                }
            }

            PrescriberDept.PIS -> {
                // Try fetching personnel
                val personnel = PisPersonnel
                    .selectAll().where { PisPersonnel.attendantId eq prescriberId.toString() }
                    .singleOrNull()

                if (personnel != null) {
                    val name = personnel[PisPersonnel.name]
                        .takeIf { it.isNotBlank() }
                        ?: PisUsers
                            .selectAll().where { PisUsers.id eq prescriberId }
                            .singleOrNull()
                            ?.get(PisUsers.username)
                        ?: "Unknown"

                    PrescriberDetails(name, dept)
                } else {
                    // fallback if no personnel record found
                    val user = PisUsers
                        .selectAll().where { PisUsers.id eq prescriberId }
                        .singleOrNull()

                    val name = user?.get(PisUsers.username) ?: "Unknown"
                    PrescriberDetails(name, dept)
                }
            }

            else -> {
                // Other departments (not implemented yet)
                PrescriberDetails("Unknown", dept)
            }
        }
    }
}

@Serializable
data class MedicationDetails(
    val id: Int,
    val drugId: Int,
    val drugName: String,              // <-- mapped drug name
    val dosageQuantity: Double,
    val packageId: Int,
    val packageName: String,           // <-- mapped package name
    val intakeInterval: Int,
    val intakeIntervalText: String,    // <-- mapped e.g., "Once daily"
    val durationValue: Int,
    val durationUnit: Int,
    val durationUnitText: String,      // <-- mapped e.g., "Days"
    val instruction: String?,
    val cost: Double = 0.00
)

@Serializable
data class PrescriptionDetails(
    val id: Int,
    val patientId: String,
    val prescriber: PrescriberDetails,
    val status: PrescriptionStatus,
    val prescribedAt: String,
    val totalCost: Double = 0.00,
    val medications: List<MedicationDetails>
)

fun mapIntakeInterval(value: Int): String {
    return when (value) {
        1 -> "Once daily"
        2 -> "Twice daily"
        3 -> "Every 8 hours"
        4 -> "Every 6 hours"
        else -> "Unknown"
    }
}

fun mapDurationUnit(value: Int): String {
    return when (value) {
        1 -> "Days"
        7 -> "Weeks"
        30 -> "Months"
        365 -> "Years"
        else -> "Unknown"
    }
}

fun getPackageName(drugId: Int, packageId: Int): String {
    val options = getDrugBasePackageAndUnits(drugId)
    return options.find { it.id == packageId }?.name ?: "Unknown Package"
}

fun getDrugName(drugId: Int): String {
    return transaction {
        Drugs
            .select(Drugs.name)
            .where { Drugs.id eq drugId }
            .singleOrNull()?.get(Drugs.name) ?: "Unknown Drug"
    }
}


fun getPrescriptionsByStatus(
    status: String,
    appointmentId: Int? = null
): List<PrescriptionDetails> {
    return transaction {
        val baseQuery = Prescriptions
            .selectAll()
            .apply {
                if (status != "ALL") {
                    val enumStatus = PrescriptionStatus.valueOf(status)
                    andWhere { Prescriptions.status eq enumStatus }
                }
                if (appointmentId != null) {
                    andWhere { Prescriptions.appointmentId eq appointmentId }
                }
            }
            .orderBy(Prescriptions.prescribedAt, SortOrder.DESC)
            .toList()

        baseQuery.map { prescriptionRow ->
            val prescriptionId = prescriptionRow[Prescriptions.id].value
            val prescriber = getPrescriberDetails(prescriptionId)

            val medications = PrescriptionMedications
                .selectAll().where { PrescriptionMedications.prescriptionId eq prescriptionId }
                .map { medRow ->
                    val drugId = medRow[PrescriptionMedications.drugId]
                    val packageId = medRow[PrescriptionMedications.packageId]

                    MedicationDetails(
                        id = medRow[PrescriptionMedications.id].value,
                        drugId = drugId,
                        drugName = getDrugName(drugId),
                        dosageQuantity = medRow[PrescriptionMedications.dosageQuantity],
                        packageId = packageId,
                        packageName = getPackageName(drugId, packageId),
                        intakeInterval = medRow[PrescriptionMedications.intakeInterval],
                        intakeIntervalText = mapIntakeInterval(medRow[PrescriptionMedications.intakeInterval]),
                        durationValue = medRow[PrescriptionMedications.durationValue],
                        durationUnit = medRow[PrescriptionMedications.durationUnit],
                        durationUnitText = mapDurationUnit(medRow[PrescriptionMedications.durationUnit]),
                        instruction = medRow[PrescriptionMedications.instruction]
                    )
                }

            PrescriptionDetails(
                id = prescriptionId,
                patientId = prescriptionRow[Prescriptions.patientId],
                prescriber = prescriber ?: PrescriberDetails("Unknown", prescriptionRow[Prescriptions.prescriberDept]),
                status = prescriptionRow[Prescriptions.status],
                prescribedAt = prescriptionRow[Prescriptions.prescribedAt].toString(),
                medications = medications
            )
        }
    }
}

fun getPrescriptionById(prescriptionId: Int): PrescriptionDetails? {
    return transaction {
        val prescriptionRow = Prescriptions
            .selectAll().where { Prescriptions.id eq prescriptionId }
            .singleOrNull() ?: return@transaction null

        val prescriber = getPrescriberDetails(prescriptionId)

        val medications = PrescriptionMedications
            .selectAll().where { PrescriptionMedications.prescriptionId eq prescriptionId }
            .map { medRow ->
                val drugId = medRow[PrescriptionMedications.drugId]
                val packageId = medRow[PrescriptionMedications.packageId]
                val quantity = medRow[PrescriptionMedications.dosageQuantity]
                val baseQuantity = medRow[PrescriptionMedications.dosageQuantity] *
                        medRow[PrescriptionMedications.intakeInterval]  *
                        medRow[PrescriptionMedications.durationValue] *
                        medRow[PrescriptionMedications.durationUnit]
                val cost = calculateTotalCost(packageId, baseQuantity.toInt())

                MedicationDetails(
                    id = medRow[PrescriptionMedications.id].value,
                    drugId = drugId,
                    drugName = getDrugName(drugId),
                    dosageQuantity = quantity,
                    packageId = packageId,
                    packageName = getPackageName(drugId, packageId),
                    intakeInterval = medRow[PrescriptionMedications.intakeInterval],
                    intakeIntervalText = mapIntakeInterval(medRow[PrescriptionMedications.intakeInterval]),
                    durationValue = medRow[PrescriptionMedications.durationValue],
                    durationUnit = medRow[PrescriptionMedications.durationUnit],
                    durationUnitText = mapDurationUnit(medRow[PrescriptionMedications.durationUnit]),
                    instruction = medRow[PrescriptionMedications.instruction],
                    cost = cost
                )
            }

        val totalCost = medications.sumOf { it.cost }

        PrescriptionDetails(
            id = prescriptionId,
            patientId = prescriptionRow[Prescriptions.patientId],
            prescriber = prescriber ?: PrescriberDetails("Unknown", prescriptionRow[Prescriptions.prescriberDept]),
            status = prescriptionRow[Prescriptions.status],
            prescribedAt = prescriptionRow[Prescriptions.prescribedAt].toString(),
            medications = medications,
            totalCost = totalCost
        )
    }
}
@Serializable
data class PrescriptionResponse(
    val success: Boolean,
    val message: String,
    val data: List<PrescriptionDetails>? = null
)

@Serializable
data class PrescriptionRequest(
    val prescriptionId: Int
)

@Serializable
data class SinglePrescriptionResponse(
    val success: Boolean,
    val message: String,
    val data: PrescriptionDetails? = null
)

@Serializable
data class DispenseResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class DispenseRequest(
    val prescriptionId: Int
)

fun getOngoingPrescriptionsCount(): Int {
    return transaction {
        Prescriptions
            .selectAll().where { Prescriptions.status eq PrescriptionStatus.ONGOING }
            .count()
            .toInt()
    }
}

fun getPrescribedPrescriptionsCount(): Int {
    return transaction {
        Prescriptions
            .selectAll().where { Prescriptions.status eq PrescriptionStatus.PRESCRIBED }
            .count()
            .toInt()
    }
}

@Serializable
data class PatientBasicInfoPIS(
    val fullName: String,
    val school: String,
    val department: String,
    val email: String
)

@Serializable
data class PatientResponse(
    val success: Boolean,
    val data: PatientBasicInfoPIS? = null,
    val error: String? = null
)
fun getPatientBasicDetailsByRegNo(regNo: String): PatientBasicInfoPIS? {
    return transaction {
        EmrPatients
            .selectAll().where { EmrPatients.regNo eq regNo }
            .limit(1)
            .map { row ->
                val fullName = listOfNotNull(
                    row[EmrPatients.surName],
                    row[EmrPatients.firstName],
                    row[EmrPatients.middleName]
                ).joinToString(" ")

                PatientBasicInfoPIS(
                    fullName = fullName,
                    school = row[EmrPatients.school],
                    department = row[EmrPatients.department],
                    email = row[EmrPatients.email]
                )
            }
            .singleOrNull()
    }
}

