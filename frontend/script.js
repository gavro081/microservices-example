let stompClient = null;
const usernameField = document.querySelector("#username");
const productField = document.querySelector("#product");
const quantityField = document.querySelector("#quantity");
const orderStatus = document.querySelector("#orderStatus");
const form = document.querySelector("#orderForm");
const submitBtn = document.querySelector("#submitBtn");
const productTable = document.querySelector("#product-table");
const productTableBody = document.querySelector("#product-table-body");
const usersTable = document.querySelector("#product-table");
const usersTableBody = document.querySelector("#users-table-body");

const submitForm = async (e) => {
	e.preventDefault();
	submitBtn.disabled = true;
	orderStatus.innerText = "Placing order";
	const username = usernameField.value;
	connectAndSubscribe(username);
	await sendOrderRequest();
	submitBtn.disabled = false;
};

const sendOrderRequest = async () => {
	const username = usernameField.value;
	const productName = productField.value;
	const quantity = quantityField.value;

	const payload = {
		username,
		productName,
		quantity,
	};

	// todo add api gateway to backend
	try {
		const response = await fetch("http://localhost:8083/orders", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(payload),
		});
		if (!response.ok) {
			throw new Error("response status: ", response.status);
		}
		const res = await response.json();
		orderStatus.innerText = "Order placed";
	} catch (err) {
		console.error(err);
	}
};

form.addEventListener("submit", submitForm);

const connectAndSubscribe = (username) => {
	// check port
	const socket = new SockJS("http://localhost:8083/ws");
	stompClient = Stomp.over(socket);

	stompClient.connect({}, (frame) => {
		const subscriptionUrl = "/topic/order-status/" + username;

		stompClient.subscribe(subscriptionUrl, (message) => {
			const update = JSON.parse(message.body);
			console.log("received message", update);
			orderStatus.innerText = update.status;

			if (update.status == "COMPLETED" || update.status == "FAILED") {
				stompClient.disconnect(() => {
					console.log("disconnected");
					loadView();
				});
			}
		});
	});
};

const loadView = async () => {
	productTableBody.innerHTML = "";
	usersTableBody.innerHTML = "";
	try {
		const response = await fetch("http://localhost:8081/products");
		const data = await response.json();
		// id, name, category, price, quantity
		renderProductsTable(data);
	} catch (err) {
		console.error("error: ", err);
		return;
	}
	try {
		const response = await fetch("http://localhost:8082/users");
		const data = await response.json();
		// id, username, balance
		renderUsersTable(data);
	} catch (err) {
		console.error("error: ", err);
		return;
	}
	document.querySelector("#tables-wrapper").style.display = "flex";
};

const renderProductsTable = (products) => {
	products.forEach((element) => {
		const tr = document.createElement("tr");
		tr.innerHTML = `
        <td>${element.name}</td>
        <td>${element.price}</td>
        <td>${element.quantity}</td>
        <td>${element.category}</td>
        `;
		productTableBody.appendChild(tr);
	});
};

const renderUsersTable = (users) => {
	users.forEach((element) => {
		const tr = document.createElement("tr");
		tr.innerHTML = `
        <td>${element.username}</td>
        <td>${element.balance}</td>
        `;
		usersTableBody.appendChild(tr);
	});
};

loadView();
