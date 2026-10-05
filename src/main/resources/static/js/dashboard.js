const page = document.body.dataset.page;
const rows = document.querySelector("[data-rows]");
const form = document.querySelector("[data-form]");
const notice = document.querySelector("[data-notice]");

const names = ["Ada Lovelace", "Grace Hopper", "Linus Torvalds", "Margaret Hamilton", "Alan Turing"];
const messages = [
  "Interview practice event received",
  "A small message moving through the system",
  "Hello from the local data playground",
  "The consumer saw this event",
  "Another message for the queue",
];

function escapeText(value) {
  return String(value ?? "");
}

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  node.textContent = escapeText(text);
  return node;
}

function date(value) {
  return value ? new Date(value).toLocaleString() : "—";
}

function render(items) {
  rows.replaceChildren();
  if (!items.length) {
    rows.append(element("p", "empty", "Nothing here yet. Add a record above to get started."));
    return;
  }

  if (page === "h2") {
    for (const item of items) {
      const tr = document.createElement("tr");
      for (const value of [item.customerName, `$${Number(item.total).toFixed(2)}`, item.status, date(item.createdAt)]) {
        tr.append(element("td", "", value));
      }
      rows.append(tr);
    }
    return;
  }

  for (const item of items) {
    const card = element("article", "record");
    const heading = element("div", "record-heading");
    heading.append(element("strong", "", item.type || item.channel || "EVENT"));
    heading.append(element("time", "", date(item.occurredAt || item.publishedAt)));
    card.append(heading);
    card.append(element("p", "record-detail", item.detail || item.message));
    if (item.messageId) card.append(element("small", "muted", `Message ID · ${item.messageId}`));
    rows.append(card);
  }
}

async function refresh() {
  const endpoint = page === "h2" ? "/api/demo/orders" : page === "mongo" ? "/api/demo/mongo" : `/api/demo/messages?channel=${page.toUpperCase()}`;
  rows.setAttribute("aria-busy", "true");
  try {
    const response = await fetch(endpoint);
    if (!response.ok) throw new Error(`Request failed (${response.status})`);
    render(await response.json());
  } catch (error) {
    rows.replaceChildren(element("p", "empty error", `${error.message}. Check that the local service is running.`));
  } finally {
    rows.removeAttribute("aria-busy");
  }
}

form?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(form));
  let endpoint;
  let payload;
  if (page === "h2") {
    endpoint = "/api/demo/orders";
    payload = { customerName: values.customerName, total: Number(values.total) };
  } else if (page === "mongo") {
    endpoint = "/api/demo/mongo";
    payload = values;
  } else {
    endpoint = "/api/demo/messages";
    payload = { channel: page.toUpperCase(), message: values.message };
  }

  const button = form.querySelector("button[type=submit]");
  button.disabled = true;
  notice.textContent = "Saving…";
  notice.className = "notice";
  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.message || `Request failed (${response.status})`);
    }
    form.reset();
    notice.textContent = page === "kafka" || page === "rabbitmq" ? "Published. Refresh in a moment to see the consumer receive it." : "Saved successfully.";
    notice.classList.add("success");
    await refresh();
  } catch (error) {
    notice.textContent = error.message;
    notice.classList.add("error");
  } finally {
    button.disabled = false;
  }
});

document.querySelector("[data-refresh]")?.addEventListener("click", refresh);
document.querySelector("[data-random]")?.addEventListener("click", () => {
  const random = (items) => items[Math.floor(Math.random() * items.length)];
  if (page === "h2") {
    form.elements.customerName.value = random(names);
    form.elements.total.value = (Math.floor(Math.random() * 9500 + 500) / 100).toFixed(2);
  } else if (page === "mongo") {
    form.elements.type.value = random(["NOTE_CREATED", "PROFILE_VIEWED", "TASK_COMPLETED", "INTERVIEW_PRACTICE"]);
    form.elements.detail.value = random(messages);
  } else {
    form.elements.message.value = random(messages);
  }
});

refresh();
