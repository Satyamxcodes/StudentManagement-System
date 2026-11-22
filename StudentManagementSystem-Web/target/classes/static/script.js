const API = "/api/students";
const tableBody = document.getElementById("studentTable");
const modal = document.getElementById("studentModal");
const form = document.getElementById("studentForm");
const statusMessage = document.getElementById("statusMessage");
let students = [];

async function loadStudents() {
    try {
        const response = await fetch(API);
        if (!response.ok) throw new Error("Could not load students.");
        students = await response.json();
        renderStudents();
        document.getElementById("totalStudents").textContent = students.length;
        document.getElementById("latestStudent").textContent = students.length
            ? students[0].name : "—";
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="5" class="empty">Unable to load records. Check Java server and MySQL.</td></tr>`;
        showStatus(error.message, true);
    }
}

function renderStudents() {
    const query = document.getElementById("searchInput").value.toLowerCase().trim();
    const filtered = students.filter(student =>
        student.name.toLowerCase().includes(query) ||
        student.email.toLowerCase().includes(query) ||
        String(student.id).includes(query)
    );

    if (!filtered.length) {
        tableBody.innerHTML = `<tr><td colspan="5" class="empty">No students found.</td></tr>`;
        return;
    }

    tableBody.innerHTML = filtered.map(student => `
        <tr>
            <td>${student.id}</td>
            <td>${escapeHtml(student.name)}</td>
            <td>${escapeHtml(student.email)}</td>
            <td>${student.age}</td>
            <td>
                <button class="action-btn" data-action="edit" data-id="${student.id}">Edit</button>
                <button class="action-btn delete" data-action="delete" data-id="${student.id}">Delete</button>
            </td>
        </tr>
    `).join("");
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, char => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[char]));
}

function openModal(student = null) {
    form.reset();
    document.getElementById("studentId").value = student ? student.id : "";
    document.getElementById("formTitle").textContent = student ? "Edit Student" : "Add Student";
    document.getElementById("saveBtn").textContent = student ? "Update Student" : "Save Student";
    if (student) {
        document.getElementById("name").value = student.name;
        document.getElementById("email").value = student.email;
        document.getElementById("age").value = student.age;
    }
    modal.classList.remove("hidden");
}

function closeModal() { modal.classList.add("hidden"); }

function showStatus(message, isError = false) {
    statusMessage.textContent = message;
    statusMessage.style.color = isError ? "#c03939" : "#287a4b";
}

document.getElementById("addNewBtn").addEventListener("click", () => openModal());
document.getElementById("closeModal").addEventListener("click", closeModal);
document.getElementById("cancelBtn").addEventListener("click", closeModal);
document.getElementById("searchInput").addEventListener("input", renderStudents);

form.addEventListener("submit", async event => {
    event.preventDefault();
    const id = document.getElementById("studentId").value;
    const data = new URLSearchParams({
        name: document.getElementById("name").value.trim(),
        email: document.getElementById("email").value.trim(),
        age: document.getElementById("age").value
    });

    try {
        const response = await fetch(id ? `${API}/${id}` : API, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: data
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || "Request failed.");
        closeModal();
        showStatus(result.message);
        await loadStudents();
    } catch (error) {
        showStatus(error.message, true);
    }
});

tableBody.addEventListener("click", async event => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    const id = Number(button.dataset.id);
    const student = students.find(item => item.id === id);

    if (button.dataset.action === "edit" && student) {
        openModal(student);
        return;
    }

    if (button.dataset.action === "delete" && confirm("Delete this student record?")) {
        try {
            const response = await fetch(`${API}/${id}`, { method: "DELETE" });
            const result = await response.json();
            if (!response.ok) throw new Error(result.message || "Delete failed.");
            showStatus(result.message);
            await loadStudents();
        } catch (error) {
            showStatus(error.message, true);
        }
    }
});

loadStudents();
