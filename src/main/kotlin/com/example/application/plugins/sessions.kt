package com.example.application.plugins

import com.example.data.models.CompletePatientData
import com.example.logic.emr.AddPatientSessionData
import com.example.logic.emr.EmrLoginSessionData
import com.example.logic.emr.PatientFolderSessionData
import com.example.logic.emr.UpdatePatientSession
import com.example.logic.lis.LisLoginSessionData
import com.example.logic.pis.PisLoginSessionData
import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.installSessions(){
    install(Sessions) {
        cookie<EmrLoginSessionData>("EmrLoginSession") { // Define a cookie for the session
            cookie.httpOnly = true
            cookie.secure = false
        }
        cookie<LisLoginSessionData>("LisLoginSession") { // Define a cookie for the session
            cookie.httpOnly = true
            cookie.secure = false
        }
        cookie<AddPatientSessionData>("AddPatientSession") { // Define a cookie for the session
            cookie.httpOnly = true
            cookie.secure = false
        }
        cookie<CompletePatientData>("PatientDetails") { // Define a cookie for the session
            cookie.httpOnly = true
            cookie.secure = false
        }
        cookie<UpdatePatientSession>("updatePatientSession") { // Define a cookie for the session
            cookie.httpOnly = true
            cookie.secure = false
        }
        cookie<PatientFolderSessionData>("PatientFolderSession") { // Define a cookie for the session
            cookie.httpOnly = true
            cookie.secure = false
        }

        cookie<PisLoginSessionData>("PisLoginSession") { // Define a cookie for the session
            cookie.httpOnly = true
            cookie.secure = false
        }

    }

}