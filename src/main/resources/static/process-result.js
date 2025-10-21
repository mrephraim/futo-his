 let commentsEditor;

  const urlParams = new URLSearchParams(window.location.search);
    const requisitionId = urlParams.get("id"); // Assuming the ID is in the URL as ?id=123
    // Initialize Quill editor
    document.addEventListener('DOMContentLoaded', function () {
        fetchAndPopulateParameters(requisitionId);
        fetchAndPopulateRequisitionDetails(requisitionId);
        fetchComments(requisitionId)
    });


     // Function to populate parameters with existing values
function populateParameters(parameters, comments) {
    const container = document.getElementById('parametersContainer');
    container.innerHTML = ''; // Clear existing inputs

    parameters.forEach(param => {
        const div = document.createElement('div');
        div.className = 'mb-3';

        let inputElement;
        let paramValue = param.value;

        // Parse the value field to extract the actual value
        if (typeof param.value === 'string') {
            if (param.value.startsWith('FloatValue(value=')) {
                // Extract float value
                paramValue = parseFloat(param.value.slice('FloatValue(value='.length, -1));
            } else if (param.value.startsWith('BooleanValue(value=')) {
                // Extract boolean value
                paramValue = param.value.slice('BooleanValue(value='.length, -1) === 'true';
            } else if (param.value.startsWith('StringValue(value="')) {
                // Extract string value
                paramValue = param.value.slice('StringValue(value="'.length, -2); // Remove `StringValue(value="` and closing `")`
            }
        }

        // Generate input element based on dataType
        if (param.dataType === "boolean") {
            // Create a <select> input for boolean data type with options for true and false
            inputElement = `
                <select class="form-control" data-type="${param.dataType}" id="parameter-${param.id}" name="parameters[${param.id}]">
                    <option value="true" ${paramValue === true ? 'selected' : ''}>True</option>
                    <option value="false" ${paramValue === false ? 'selected' : ''}>False</option>
                </select>
            `;
        } else if (param.dataType === "number") {
            // Create a number input
            inputElement = `
                <input type="number" class="form-control"
                    id="parameter-${param.id}" data-type="${param.dataType}" name="parameters[${param.id}]"
                    value="${paramValue || ''}">
            `;
        } else if (param.dataType === "string") {
            // Create a text input
            inputElement = `
                <input type="text" class="form-control"
                    id="parameter-${param.id}" data-type="${param.dataType}" name="parameters[${param.id}]"
                    value="${paramValue || ''}">
            `;
        }

        if (param.dataType === "boolean" || param.dataType === "string") {
            // Add the parameter input, label, and tooltip
            div.innerHTML = `
                <div class="d-flex justify-content-between align-items-center">
                    <label for="parameter-${param.id}" class="form-label"
                        data-bs-toggle="tooltip" title="${param.description}">
                        ${param.name}
                    </label>
                </div>
                ${inputElement}
            `;
            container.appendChild(div);
        } else {
            // Add the parameter input, label, and tooltip for number fields
            div.innerHTML = `
                <div class="d-flex justify-content-between align-items-center">
                    <label for="parameter-${param.id}" class="form-label"
                        data-bs-toggle="tooltip" title="${param.description}">
                        ${param.name} (${param.units.name || ""})
                    </label>
                    <button type="button" id="convertButton" class="btn btn-sm btn-secondary"
                        onclick="convertValue(${param.id}, ${param.units.factor}, '${param.units.base}')">
                        Convert to ${param.units.base}
                    </button>
                </div>
                ${inputElement}
            `;
            container.appendChild(div);
        }
    });

    // Initialize Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Disable number input spinner controls (CSS fix)
    const numberInputs = document.querySelectorAll('input[type="number"]');
    numberInputs.forEach(input => {
        input.addEventListener('mousewheel', function(event) {
            event.preventDefault(); // Prevent scrolling inside the input field
        });

        // Disable the spinner controls
        input.style.webkitAppearance = 'none'; // For Webkit-based browsers like Chrome
        input.style.mozAppearance = 'textfield'; // For Firefox
        input.style.appearance = 'none'; // For other browsers
    });
}

// Function to handle value conversion
function convertValue(parameterId, conversionFactor, baseUnit) {
    const input = document.getElementById(`parameter-${parameterId}`);
    if (!input || isNaN(input.value)) {
        alert("Please enter a valid value to convert.");
        return;
    }

    const originalValue = parseFloat(input.value);
    const convertedValue = originalValue * conversionFactor;
    alert(`Converted Value (${baseUnit}): ${convertedValue.toFixed(2)}`);
}

// Fetch parameters from the server and populate
async function fetchAndPopulateParameters(id) {
    try {
        const response = await fetch(`/lis/api/get-parameters/${id}`);
        if (!response.ok) throw new Error(`Error: ${response.statusText}`);

        const data = await response.json();

        // Ensure parameters exist and are an array
        if (Array.isArray(data.parameters)) {
            populateParameters(data.parameters);
        } else {
            console.error("Parameters data is invalid:", data.parameters);
        }
    } catch (error) {
        console.error("Failed to fetch parameters:", error);
    }
}




    // Fetch previous comments asynchronously
    async function fetchComments(requisitionId) {
        try {
            const response = await fetch(`/lis/api/requisitions/comments/${requisitionId}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            });

            if (response.ok) {
                const comments = await response.json();
                populateComments(comments);
            } else {
                console.error('Error fetching comments:', response.statusText);
                alert('Failed to load comments.');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('An error occurred while fetching comments.');
        }
    }

    // Populate comments with delete buttons
    function populateComments(comments) {
        const container = document.getElementById('commentsContainer');
        container.innerHTML = ''; // Clear existing comments

        comments.forEach(comment => {
            const formattedTime = formatDateTime(comment.time);
            const div = document.createElement('div');
            div.className = 'comment-item';
            div.innerHTML = `
                <p>${comment.comment} <br/><small>(${formattedTime})</small></p>
                <button class="btn btn-sm btn-danger" onclick="deleteComment(${comment.id})">Delete</button>
            `;
            container.appendChild(div);
        });
    }

    // Delete a comment asynchronously
    async function deleteComment(commentId) {
    event.preventDefault();
     const requisitionId = new URLSearchParams(window.location.search).get("id");
        try {
            // Send DELETE request to the server
            const response = await fetch(`/api/comments/${requisitionId}/${commentId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            if (response.ok) {
                const result = await response.json();
                alert(result.message); // Show success message
                window.location.reload();
            } else {
                const errorText = await response.text();
                alert(`Failed to delete comment: ${errorText}`);
            }
        } catch (error) {
            console.error("Error deleting comment:", error);
            alert("An error occurred while deleting the comment.");
        }
    }


    // Function to fetch and populate patient requisition details
    async function fetchAndPopulateRequisitionDetails(requisitionId) {
        try {
            // Fetch requisition details from the server
            const response = await fetch(`/lis/api/requisition/${requisitionId}`);
            if (!response.ok) throw new Error(`Error: ${response.statusText}`);

            const requisitionData = await response.json();

            // Populate the form fields with the fetched data
            document.getElementById('patientName').value = requisitionData.fullName || 'N/A';
            document.getElementById('patientRegNo').value = requisitionData.patientRegNo || 'N/A';
            document.getElementById('testName').value = requisitionData.labTestId || 'N/A'; // Replace with actual test name if available
            document.getElementById('sampleType').value = requisitionData.sampleTypeId || 'N/A'; // Replace with actual sample type if available
            document.getElementById('sampleId').value = requisitionData.sampleId || 'N/A';
            document.getElementById('sampleCollectionDate').value = requisitionData.collectionDateTime || 'N/A';
        } catch (error) {
            console.error("Failed to fetch and populate requisition details:", error);
        }
    }

// Function to handle asynchronous form submission with loader
async function submitForm(action) {
    // Get requisition ID from the query string
    const requisitionId = new URLSearchParams(window.location.search).get("id");

    // Fetch existing comments from the server
    let existingComments = [];
    try {
        const response = await fetch(`/lis/api/requisitions/comments/${requisitionId}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
            existingComments = await response.json(); // Make sure the response is in the expected format
        } else {
            console.error('Error fetching comments:', response.statusText);
            alert('Failed to load comments.');
        }
    } catch (error) {
        console.error('Error fetching comments:', error);
        alert('An error occurred while fetching comments.');
    }

    // Set the status based on the action
    const status = action === 'save' ? 1 : 2;

    // Collect parameters and ensure correct types
    const parameters = [];
    document.querySelectorAll('input[name^="parameters["], select[name^="parameters["]').forEach(input => {
        const paramId = input.name.match(/\[(\d+)\]/)[1]; // Extract parameter ID
        const paramDataType = input.getAttribute('data-type'); // Retrieve data-type from the input

        let paramValue;

        if (paramDataType === "boolean") {
            // For boolean, ensure we store a true/false value
            paramValue = { type: "boolean", value: input.value === "true" };
        } else if (paramDataType === "number" || paramDataType === "integer") {
            // For numbers, parse as float
            paramValue = { type: "float", value: parseFloat(input.value) || 0 };
        } else {
            // For other types (strings), store as a string
            paramValue = { type: "string", value: input.value.trim() };
        }
        parameters.push(paramValue);
    });

    console.log("Collected Parameters:", parameters); // Debugging the collected parameters

    // Collect the new comment/report from the editor
    const comment = document.getElementById('commentsEditor').value.trim();
    const comments = [];

    if (comment) {
        const currentDate = new Date().toISOString(); // Add the current timestamp

        // Determine the starting ID based on existing comments
        const startingId = existingComments.length > 0
            ? existingComments[existingComments.length - 1].id + 1 // Start from the next available ID
            : 1; // Default to 1 if no existing comments

        // Add the new comment with an incremental ID
        comments.push({
            id: startingId,
            comment: comment,
            time: currentDate
        });
    }

    // Construct the JSON payload
    const payload = {
        status,
        requisitionId: parseInt(requisitionId, 10),
        parameters, // Send as the correctly typed array
        comments
    };

    console.log("Payload to send:", payload); // Debugging the payload

    // Get the button element and display a loader
    const button = action === 'save'
        ? document.querySelector('.btn-outline-success')
        : document.querySelector('.btn-success');

    const originalText = button.innerHTML;
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...';
    button.disabled = true;

    try {
        // Send the JSON payload to the server
        const response = await fetch('/lis/api/submit-result-form', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
        });

        // Handle the server response
        if (response.ok) {
            const result = await response.json();

            // Alert success message
            alert(`Action completed successfully: ${result.message}`);

            if (action === 'publish') {
                // Redirect to the result printing page
//                window.location.href = `/lis/results/print/${requisitionId}`;
                window.location.reload();
            } else {
                // Reload the current page for 'Save'
                window.location.reload();
            }
        } else {
            const errorText = await response.text();
            alert(`Failed to complete the action: ${errorText}`);
        }
    } catch (error) {
        console.error("Error submitting form:", error);
        alert("An error occurred while submitting the form.");
    } finally {
        // Reset the button to its original state
        button.innerHTML = originalText;
        button.disabled = false;
    }
}


function getParameterDataType(paramId) {
    // Fetch the dataType for the parameter based on its ID
    // You can store and retrieve this info from the parameters list
    const param = parameters.find(p => p.id === parseInt(paramId));
    return param ? param.dataType : "text"; // Default to "text" if not found
}

function formatDateTime(dateString) {
    const date = new Date(dateString);

    // Check if the date is valid
    if (isNaN(date.getTime())) {
        return 'Invalid date';
    }

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear().toString().slice(-2); // Get last 2 digits of the year
    const hours = date.getHours();
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const ampm = hours >= 12 ? 'pm' : 'am';

    const formattedDate = `${day}-${month}-${year} ${hours % 12 || 12}:${minutes} ${ampm}`;
    return formattedDate;
}
