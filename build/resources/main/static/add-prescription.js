async function loadDrugs(selectEl) {
    try {
      const res = await fetch('/pis/drugs');
      const drugs = await res.json();
      selectEl.innerHTML = '<option value="">-- Select Drug --</option>';
      drugs.forEach(d => {
        const opt = document.createElement('option');
        opt.value = d.id;
        opt.textContent = d.name;
        selectEl.appendChild(opt);
      });
    } catch (err) {
      console.error('Error fetching drugs', err);
    }
  }

  // Load dosage units when drug changes
  async function loadDosageUnits(drugId, unitSelect) {
    if (!drugId) return;
    try {
      const res = await fetch(`/pis/prescriptions/${drugId}/dosage-units`);
      const units = await res.json();

      unitSelect.innerHTML = '<option value="">-- Select Unit --</option>';

      units.forEach(u => {
        const opt = document.createElement('option');
        opt.value = u.id;
        opt.textContent = `${u.name} (ID: ${u.id})`;
        unitSelect.appendChild(opt);
      });
    } catch (err) {
      console.error('Error fetching units', err);
    }
  }


  // Add medication row
  document.getElementById('addMedicationBtn').addEventListener('click', function () {
    const container = document.getElementById('medicationsContainer');
    const firstRow = container.querySelector('.medication-row');
    const newRow = firstRow.cloneNode(true);

    // Clear values
    newRow.querySelectorAll('input, textarea').forEach(el => el.value = '');
    newRow.querySelectorAll('select').forEach(el => el.selectedIndex = 0);

    // Append and reload drug list
    container.appendChild(newRow);
    loadDrugs(newRow.querySelector('.drug-select'));
  });

  // Delegate remove button
  document.getElementById('medicationsContainer').addEventListener('click', function (e) {
    if (e.target.closest('.remove-medication-btn')) {
      if (document.querySelectorAll('.medication-row').length > 1) {
        e.target.closest('.medication-row').remove();
      }
    }
  });

  // Delegate drug change event
  document.getElementById('medicationsContainer').addEventListener('change', function (e) {
    if (e.target.classList.contains('drug-select')) {
      const drugId = e.target.value;
      const unitSelect = e.target.closest('.medication-row').querySelector('.dosage-unit');
      loadDosageUnits(drugId, unitSelect);
    }
  });

  // Initial load for first row
  document.addEventListener('DOMContentLoaded', () => {
    loadDrugs(document.querySelector('.drug-select'));
  });

      // Handle form submission
    // Submit Prescription
    document.getElementById('createPrescriptionBtn').addEventListener('click', async function () {
        const btn = this;
        const originalText = btn.innerHTML;

        // Show spinner
        btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span> Creating...`;
        btn.disabled = true;

        const patientId = document.getElementById('patientInfo').value.trim();
        const prescriberId = parseInt(document.getElementById('prescriberId').value || 0);

        // ✅ Department check with fallback
        const deptInput = document.getElementById('prescriberDept');
        const prescriberDept = deptInput ? (deptInput.value.trim() || "PIS") : "PIS";

        // ✅ AppointmentId check in URL
        const urlParams = new URLSearchParams(window.location.search);
        const appointmentId = urlParams.get("id") ? parseInt(urlParams.get("id")) : null;

        const medications = [];
        document.querySelectorAll('.medication-row').forEach(row => {
            medications.push({
                drugId: parseInt(row.querySelector('.drug-select').value || 0),
                dosageQuantity: parseFloat(row.querySelector('.dosage-value').value || 0),
                packageId: parseInt(row.querySelector('.dosage-unit').value || 0),
                intakeInterval: parseInt(row.querySelector('.frequency').value || 0),
                durationValue: parseInt(row.querySelector('.duration-value').value || 0),
                durationUnit: parseInt(row.querySelector('.duration-unit').value || 0),
                instruction: row.querySelector('.instructions').value || null
            });
        });

        // ✅ Build payload dynamically
        const payload = {
            patientId,
            prescriberId,
            prescriberDept,
            medications
        };

        if (appointmentId) {
            payload.appointmentId = appointmentId; // only attach if available
        }

        try {
            const res = await fetch('/pis/prescriptions/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await res.json();
            if (result.success) {
                alert(`✅ ${result.message}. Prescription ID: ${result.prescriptionId}`);
                location.reload(); // refresh to show new prescription
            } else {
                alert(`❌ ${result.message}`);
            }
        } catch (err) {
            console.error('Error submitting prescription', err);
            alert('An unexpected error occurred while creating prescription.');
        } finally {
            // Restore button
            btn.innerHTML = originalText;
            btn.disabled = false;
        }
    });

  document.addEventListener("DOMContentLoaded", () => {
      const input = document.getElementById("patientInfo");
      const loader = document.getElementById("patientLoader");
      const details = document.getElementById("patientDetails");
      const errorBox = document.getElementById("patientError");
      const removeBtn = document.getElementById("removePatientBtn");

      async function fetchPatient(regNo) {
          loader.classList.remove("d-none");
          details.classList.add("d-none");
          errorBox.classList.add("d-none");

          try {
              const res = await fetch("/pis/prescriptions/patient-info", {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({ regNo })
              });

              loader.classList.add("d-none");

              if (!res.ok) {
                  errorBox.textContent = res.status === 404
                      ? "Patient not found."
                      : "Error fetching patient info.";
                  errorBox.classList.remove("d-none");
                  return;
              }

              const data = await res.json();

              // Fill details
              document.getElementById("pdName").textContent = data.fullName;
              document.getElementById("pdDob").textContent = data.dob || "-";
              document.getElementById("pdSex").textContent = data.sex;
              document.getElementById("pdSchool").textContent = data.school || "-";
              document.getElementById("pdDepartment").textContent = data.department || "-";
              document.getElementById("pdPhone").textContent = data.phoneNo || "-";
              document.getElementById("pdEmail").textContent = data.email || "-";
              document.getElementById("pdWeight").textContent = data.weight || "-";
              document.getElementById("pdHeight").textContent = data.height || "-";
              document.getElementById("pdBloodGroup").textContent = data.bloodGroup || "-";
              document.getElementById("pdGenotype").textContent = data.genotype || "-";
              document.getElementById("pdAllergies").textContent = data.allergies || "-";
              document.getElementById("pdIllness").textContent = data.previousIllness || "-";

              details.classList.remove("d-none");

          } catch (err) {
              loader.classList.add("d-none");
              errorBox.textContent = "Network error.";
              errorBox.classList.remove("d-none");
          }
      }

      // Enter triggers fetch
      input.addEventListener("keypress", (e) => {
          if (e.key === "Enter") {
              e.preventDefault();
              const regNo = input.value.trim();
              if (regNo) fetchPatient(regNo);
          }
      });

      // Remove patient
      removeBtn.addEventListener("click", () => {
          details.classList.add("d-none");
          input.value = "";
      });
  });
