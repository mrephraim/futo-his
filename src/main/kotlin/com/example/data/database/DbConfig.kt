package com.example.data.database


import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database


fun Application.configureDatabases() {

    Database.connect(
        url = "jdbc:postgresql://dpg-d40ithbipnbc73b36af0-a:5432/futo_his_pgdb",
        driver = "org.postgresql.Driver",
        user = "futo_his_pgdb_user",
        password = "4cgdV1K6l7ls9eEkl63N0YR7RPQj507G"
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
