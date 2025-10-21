package com.example.data.database


import org.jetbrains.exposed.sql.Table

object EmrPatients : Table() {
    val id = integer("id").autoIncrement() // Primary Key
    val regNo = varchar("reg_no", 11).uniqueIndex() // Unique Registration Number
    val surName = varchar("sur_name", 50)
    val firstName = varchar("first_name", 50)
    val middleName = varchar("middle_name", 50).nullable() // Middle name is optional
    val school = varchar("school", 100)
    val department = varchar("department", 100)
    val phoneNo = varchar("phone_no", 15)
    val email = varchar("email", 100)
    val dob = varchar("dob", 10)
    val sex = varchar("sex", 10) // Can be "Male", "Female", etc.
    val maritalStatus = varchar("marital_status", 20) // Can be "Single", "Married", etc.
    val hostelAddress = varchar("hostel_address", 150).nullable()
    val homeTown = varchar("home_town", 100)
    val lga = varchar("lga", 100) // Local Government Area
    val state = varchar("state", 100)
    val country = varchar("country", 100)

    // Specify the primary key for the table
    override val primaryKey = PrimaryKey(id)
}
object EmrPatientsGuardians : Table() {
    val regNo = varchar("reg_no", 11).uniqueIndex() // Unique Registration Number
    val name = varchar("name", 100) // Guardian's Name
    val phoneNo = varchar("phone_no", 15) // Phone Number
    val officeAddress = varchar("office_address", 255) // Office Address
    val residentialAddress = varchar("residential_address", 255) // Residential Address
    val occupation = varchar("occupation", 100) // Occupation
}


object EmrPatientsKins : Table() {
    val regNo = varchar("reg_no", 11).uniqueIndex() // Unique Registration Number
    val name = varchar("name", 100) // Kin's Name
    val phoneNo = varchar("phone_no", 255) // Office Address
    val residentialAddress = varchar("residential_address", 255) // Residential Address
    val occupation = varchar("occupation", 100) // Occupation
    val relationToStudent = varchar("relation_to_student", 100) // Relation to Student
}

object EmrPatientMedicalHistory : Table() {
    val regNo = varchar("reg_no", 11).uniqueIndex() // Unique Registration Number
    val familyHistory = varchar("family_history", 255) // Family history of illnesses
    val previousIllness = varchar("previous_illness", 255) // Details of previous illnesses
    val height = decimal("height", 5, 2) // Height in meters or centimeters
    val weight = decimal("weight", 5, 2) // Weight in kilograms
    val visualAcuity = varchar("visual_acuity", 50) // Visual acuity description or measurement
    val cardiovascularHealth = varchar("cardiovascular_health", 255) // Heart health information
    val hearingCondition = varchar("hearing_condition", 255) // Description of hearing condition
    val abdominalHealth = varchar("abdominal_health", 255) // Abdominal health status
    val respiratoryHealth = varchar("respiratory_health", 255) // Respiratory condition
    val extremities = varchar("extremities", 255) // Extremities condition
    val psychiatricState = varchar("psychiatric_state", 255) // Psychiatric state
    val genotype = varchar("genotype", 255) // Genotype
    val bloodGroup = varchar("blood_group", 255) // Blood group
    val chestXray = varchar("chest_xray", 255) // Chest X-ray information
}

