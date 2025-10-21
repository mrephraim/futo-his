package com.example.logic.lis

import com.example.data.database.EmrDoctors
import com.example.data.database.EmrPatients
import com.example.data.database.Requisitions
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import jakarta.mail.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage



fun loadConfig(): ApplicationConfig {
    return HoconApplicationConfig(ConfigFactory.load()) // Loads application.conf
}

fun getEmailCredentials(): Pair<String, String> {
    val config = loadConfig()
    val email = config.propertyOrNull("ktor.email.address")?.getString() ?: "DEFAULT_EMAIL"
    val emailPassword = config.propertyOrNull("ktor.email.password")?.getString() ?: "DEFAULT_PASSWORD"
    return email to emailPassword
}


// Query Requisitions table to get regNo and physicianId
fun getRequisitionDetails(requisitionId: Int): Triple<String, Int?, String>? = transaction {
    Requisitions
        .select(Requisitions.patientRegNo, Requisitions.physicianId, Requisitions.labTest)
        .where { Requisitions.id eq requisitionId }
        .map { Triple(it[Requisitions.patientRegNo], it[Requisitions.physicianId] , it[Requisitions.labTest]) }
        .firstOrNull()
}

// Query EmrPatients table to get patient email and name
fun getPatientDetails(regNo: String): Triple<String, String, String>? = transaction {
    EmrPatients
        .select(EmrPatients.firstName, EmrPatients.surName, EmrPatients.email) // Select all three fields
        .where { EmrPatients.regNo eq regNo }
        .map { Triple(it[EmrPatients.firstName], it[EmrPatients.surName], it[EmrPatients.email]) } // Map to Triple
        .firstOrNull()
}


// Query EmrDoctors table to get doctor email
fun getDoctorEmail(physicianId: String): String? = transaction {
    EmrDoctors
        .select(EmrDoctors.email) // Use slice() instead of select() for a single column
        .where { EmrDoctors.doctorId eq physicianId }
        .map{ it[EmrDoctors.email] } // Ensure non-null mapping
        .firstOrNull()
}


fun sendEmail(mSubject: String, body: String, toEmails: List<String>, fromEmail: String, password: String): Boolean {
    val properties = Properties().apply {
        put("mail.smtp.host", "mail.abetn.org.ng") // Correct host (remove "ssl://")
        put("mail.smtp.port", "465") // Correct SSL port
        put("mail.smtp.auth", "true") // Enable authentication
        put("mail.smtp.ssl.enable", "true") // Explicitly enable SSL
        put("mail.smtp.starttls.enable", "false") // STARTTLS is not needed with SSL
        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory") // Ensure SSL socket
        put("mail.smtp.auth.mechanisms", "LOGIN") // Explicitly set LOGIN authentication
//        put("mail.debug", "true") // Enable debugging
    }

    try {
        // Set up authentication
        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(fromEmail, password) // Ensure this is correct
            }
        })

        session.debug = true // Enable detailed logs

        // Create the email message
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(fromEmail))
            toEmails.forEach { addRecipient(Message.RecipientType.TO, InternetAddress(it)) }
            subject = mSubject
            setText(body)
        }

        // Send the email
        Transport.send(message)
        println("✅ Email sent successfully to: $toEmails")
        return true
    } catch (e: MessagingException) {
        e.printStackTrace()
        println("❌ Failed to send email.")
        return false
    }
}
