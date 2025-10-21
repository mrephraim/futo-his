    document.addEventListener("DOMContentLoaded", () => {
        let unitIndex = 1;
        let commentIndex = 1;

        // Add new unit row
        document.getElementById("add-unit").addEventListener("click", () => {
            const unitRow = `
                <div class="unit-group input-group mb-2">
                    <input type="text" class="form-control unit-name" name="unit[${unitIndex}][name]" placeholder="Unit (e.g., g/dL)" required>
                    <input type="text" class="form-control unit-base" name="unit[${unitIndex}][base]" placeholder="Base Unit" required>
                    <input type="number" step="any" class="form-control unit-factor" name="unit[${unitIndex}][factor]" placeholder="Multiplication Factor" required>
                    <span class="example ms-2 mt-2"></span>
                </div>`;
            document.getElementById("units-container").insertAdjacentHTML("beforeend", unitRow);
            unitIndex++;
        });

        // Add new comment row
        document.getElementById("add-comment").addEventListener("click", () => {
            const commentRow = `
                <input type="text" class="form-control mb-2" name="comment[${commentIndex}]" placeholder="Add a comment">`;
            document.getElementById("comments-container").insertAdjacentHTML("beforeend", commentRow);
            commentIndex++;
        });

        // Update conversion example dynamically
        document.getElementById("units-container").addEventListener("input", (e) => {
            if (e.target.closest(".unit-group")) {
                const unitGroup = e.target.closest(".unit-group");
                const unitName = unitGroup.querySelector(".unit-name").value.trim();
                const baseUnit = unitGroup.querySelector(".unit-base").value.trim();
                const factor = unitGroup.querySelector(".unit-factor").value.trim();

                if (unitName && baseUnit && factor) {
                    unitGroup.querySelector(".example").textContent = `1 ${unitName} = ${factor} ${baseUnit}`;
                } else {
                    unitGroup.querySelector(".example").textContent = "";
                }
            }
        });

        // Form submission
        document.getElementById("parameter-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const formData = new FormData(e.target);

            // Construct JSON manually
            const jsonData = {
                parameterName: formData.get("parameterName"),
                dataType: formData.get("dataType"),
                description: formData.get("description"),
                units: [],
                comments: []
            };

            // Process units
            formData.forEach((value, key) => {
                const unitMatch = key.match(/^unit\[(\d+)]\[(\w+)]$/);
                if (unitMatch) {
                    const [_, index, field] = unitMatch;
                    const unitIndex = parseInt(index, 10);
                    if (!jsonData.units[unitIndex]) {
                        jsonData.units[unitIndex] = { name: "", base: "", factor: 0 };
                    }
                    jsonData.units[unitIndex][field] = field === "factor" ? parseFloat(value) : value;
                }
            });

            // Process comments
            formData.forEach((value, key) => {
                const commentMatch = key.match(/^comment\[(\d+)]$/);
                if (commentMatch) {
                    jsonData.comments.push(value);
                }
            });

            // Submit JSON to the backend
            try {
                const response = await fetch("/lis/add-parameter", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(jsonData),
                });

                const result = await response.json();
                if (response.ok) {
                    alert("Parameter added successfully!");
                    e.target.reset();
                } else {
                    alert(`Error: ${result.error || "Something went wrong"}`);
                }
            } catch (error) {
                console.error("AJAX error:", error);
                alert("Failed to submit form.");
            }
        });
    });