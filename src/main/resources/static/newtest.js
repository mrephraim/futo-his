document.addEventListener('DOMContentLoaded', async () => {
    // Fetch data from the server
    const fetchData = async (endpoint) => {
        try {
            const response = await fetch(endpoint);
            if (!response.ok) throw new Error('Failed to fetch data');
            return await response.json();
        } catch (error) {
            console.error(error);
            return [];
        }
    };

    // Fetch lab parameters and sample types
    const parametersData = await fetchData('/lis/api/lab-parameters'); // Replace with the correct endpoint
    const sampleTypesData = await fetchData('/lis/api/lab-samples'); // Replace with the correct endpoint

     // Fetch categories dynamically
        const fetchCategories = async () => {
            try {
                const response = await fetch('/lis/categories'); // Replace with the actual endpoint
                if (!response.ok) throw new Error('Failed to fetch categories');
                return await response.json();
            } catch (error) {
                console.error(error);
                return [];
            }
        };


    const renderOptions = (data, containerId, selectedContainerId) => {
        const container = document.getElementById(containerId);
        const selectedContainer = document.getElementById(selectedContainerId);

        container.innerHTML = ''; // Clear previous options

        data.forEach(({ id, name }) => {
            const uniqueId = `${containerId}-${id}`;
            const option = document.createElement('li');
            option.classList.add('dropdown-item');
            option.innerHTML = `
                <div class="form-check">
                    <input class="form-check-input" type="checkbox" name="${containerId}" value="${id}" id="${uniqueId}">
                    <label class="form-check-label" for="${uniqueId}">${name}</label>
                </div>
            `;

            option.querySelector('input').addEventListener('change', function () {
                if (this.checked) {
                    const pill = document.createElement('span');
                    pill.classList.add('badge', 'rounded-pill', 'bg-success', 'me-1', 'mb-1');
                    pill.textContent = name;
                    pill.id = `pill-${uniqueId}`;
                    const closeBtn = document.createElement('button');
                    closeBtn.type = 'button';
                    closeBtn.classList.add('btn-close', 'btn-close-white', 'ms-2');
                    closeBtn.setAttribute('aria-label', 'Close');
                    closeBtn.addEventListener('click', () => {
                        document.getElementById(uniqueId).checked = false;
                        pill.remove();
                    });
                    pill.appendChild(closeBtn);
                    selectedContainer.appendChild(pill);
                } else {
                    const pill = document.getElementById(`pill-${uniqueId}`);
                    if (pill) pill.remove();
                }
            });

            container.appendChild(option);
        });
    };

    const setupSearch = (searchInputSelector, optionsContainerSelector) => {
        const searchInput = document.querySelector(searchInputSelector);
        const optionsContainer = document.querySelector(optionsContainerSelector);

        if (searchInput && optionsContainer) {
            searchInput.addEventListener('input', () => {
                const searchValue = searchInput.value.toLowerCase();
                const options = optionsContainer.querySelectorAll('.dropdown-item');
                options.forEach((option) => {
                    const label = option.querySelector('.form-check-label').textContent.toLowerCase();
                    option.style.display = label.includes(searchValue) ? '' : 'none';
                });
                optionsContainer.style.display = searchValue ? 'block' : 'none';
            });

            searchInput.addEventListener('click', () => {
                optionsContainer.style.display = 'block';
            });

            document.addEventListener('click', (e) => {
                if (!searchInput.contains(e.target)) {
                    optionsContainer.style.display = 'none';
                }
            });
        } else {
            console.error(`Could not find element(s) with selectors "${searchInputSelector}" or "${optionsContainerSelector}".`);
        }
    };

    // Render options for Parameters and Sample Types
    renderOptions(parametersData, 'parametersOptions', 'parametersSelected');
    renderOptions(sampleTypesData, 'sampleTypesOptions', 'sampleTypesSelected');

    // Setup search functionality for both dropdowns
    setupSearch('#searchParameters', '#parametersOptions');
    setupSearch('#searchSampleTypes', '#sampleTypesOptions');

    const categoriesData = await fetchCategories();
        if (categoriesData.length > 0) {
            const categorySelect = document.getElementById('testCategory');
            categoriesData.forEach(category => {
                const option = document.createElement('option');
                option.value = category.id;
                option.text = category.name;
                categorySelect.appendChild(option);
            });
        }

    document.getElementById('labTestForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const loader = document.createElement('div');
        loader.className = 'spinner-border text-primary';
        loader.setAttribute('role', 'status');
        const button = e.target.querySelector('button[type="submit"]');
        button.disabled = true;
        button.textContent = 'Submitting...';
        button.appendChild(loader);

        const formData = new FormData(e.target);
        const data = {
            testName: formData.get('testName'),
            testCategory: formData.get('testCategory'),
            bsl: formData.get('biohazardLevel'),
            parameters: Array.from(formData.getAll('parametersOptions')),
            sampleTypes: Array.from(formData.getAll('sampleTypesOptions'))
        };

        try {
            const response = await fetch('/lis/submit-lab-test', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                const result = await response.json();
                alert(`Lab test submitted successfully `);
                e.target.reset();
                // Clear all pills in selected containers
                const selectedContainers = document.querySelectorAll('#parametersSelected, #sampleTypesSelected');
                selectedContainers.forEach(container => container.innerHTML = '');
            } else {
                const error = await response.json();
                alert(`Error: ${error.message}`);
            }
        } catch (error) {
            console.error('Submission failed', error);
            alert('Submission failed. Please try again.');
        } finally {
            button.disabled = false;
            button.textContent = 'Submit';
            loader.remove();
        }
    });

});


