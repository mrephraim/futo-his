function populateDepartments(school) {
    const departmentSelect = document.getElementById('departments');
    departmentSelect.innerHTML = ''; // Clear previous options

    let departments = [];

    switch (school) {
        case 'SAAT':
            departments = [
                "Agribusiness",
                "Agricultural Economics",
                "Agricultural Extension",
                "Animal Science and Technology",
                "Crop Science and Technology",
                "Fisheries and Aquaculture Technology",
                "Forestry and Wildlife Technology",
                "Soil Science and Technology"
            ];
            break;
        case 'SBMS':
            departments = [
                "Human Anatomy",
                "Human Physiology"
            ];
            break;
        case 'SOBS':
            departments = [
                "Biochemistry",
                "Biology",
                "Biotechnology",
                "Microbiology",
                "Forensic Science"
            ];
            break;
        case 'SEET':
            departments = [
                "Agricultural and Bio resources Engineering",
                "Biomedical Engineering",
                "Chemical Engineering",
                "Civil Engineering",
                "Food Science and Technology",
                "Material and Metallurgical Engineering",
                "Mechanical Engineering",
                "Petroleum Engineering",
                "Polymer and Textile Engineering"
            ];
            break;
        case 'SESET':
            departments = [
                "Computer Engineering",
                "Electrical (Power Systems) Engineering",
                "Electronics Engineering",
                "Mechatronics Engineering",
                "Telecommunications Engineering",
                "Electrical and Electronic Engineering"
            ];
            break;
        case 'SOES':
            departments = [
                "Architecture",
                "Building Technology",
                "Environmental Management",
                "Quantity Surveying",
                "Surveying and Geoinformatics",
                "Urban and Regional Planning",
                "Environmental Management and Evaluation"
            ];
            break;
        case 'SOHT':
            departments = [
                "Dental Technology",
                "Environmental Health Science",
                "Optometry",
                "Prosthetics and Orthotics",
                "Public Health Technology"
            ];
            break;
        case 'SICT':
            departments = [
                "Computer Science",
                "Cyber Security",
                "Information Technology",
                "Software Engineering"
            ];
            break;
        case 'SLIT':
            departments = [
                "Entrepreneurship and Innovation",
                "Logistics and Transport Technology",
                "Maritime Technology and Logistics",
                "Supply Chain Management",
                "Project Management Technology"
            ];
            break;
        case 'SOPS':
            departments = [
                "Chemistry",
                "Geology",
                "Mathematics",
                "Physics",
                "Science Laboratory Technology",
                "Statistics"
            ];
            break;
        case 'SPGS':
            departments = [
                "PG School",
                "Directorate of General Studies",
                "General Studies"
            ];
            break;
        default:
            break;
    }

    // Populate the department select options
    if (departments.length > 0) {
        departments.forEach(function(department) {
            const option = document.createElement('option');
            option.value = department;
            option.textContent = department;
            departmentSelect.appendChild(option);
        });
    } else {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = '--No Departments Available--';
        departmentSelect.appendChild(option);
    }
}