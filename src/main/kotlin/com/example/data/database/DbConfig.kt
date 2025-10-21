package com.example.data.database


import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database


fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/futo-his",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "Ephraim"
    )

    //setup database tables
    createTableIfNotExists(EmrUsers)
    createTableIfNotExists(EmrDoctors)
    createTableIfNotExists(EmrPatients)
    createTableIfNotExists(EmrPatientsGuardians)
    createTableIfNotExists(EmrPatientsKins)
    createTableIfNotExists(EmrPatientMedicalHistory)
    createTableIfNotExists(EmrAppointments)
    createTableIfNotExists(LisUsers)
    createTableIfNotExists(LabCategories)
    createTableIfNotExists(LabParameters)
    createTableIfNotExists(LabParameterUnits)
    createTableIfNotExists(LabParameterComments)
    createTableIfNotExists(LabSampleTable)
    createTableIfNotExists(LabTests)
    createTableIfNotExists(LabAttendants)
    createTableIfNotExists(Requisitions)
    createTableIfNotExists(LabResults)
    createTableIfNotExists(BioHazardIncidents)
    createTableIfNotExists(LabOrders)
    createTableIfNotExists(PisUsers)
    createTableIfNotExists(PisPersonnel)
    createTableIfNotExists(DrugBatches)
    createTableIfNotExists(DrugCategories)
    createTableIfNotExists(DrugGenerics)
    createTableIfNotExists(Drugs)
    createTableIfNotExists(DrugParameters)
    createTableIfNotExists(Units)
    createTableIfNotExists(DrugPackages)
    createTableIfNotExists(DosageForms)
    createTableIfNotExists(InventoryBatches)
    createTableIfNotExists(Prescriptions)
    createTableIfNotExists(PrescriptionMedications)





}
