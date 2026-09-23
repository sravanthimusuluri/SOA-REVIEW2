/**
 * Bibliotech Circulation Systems - Application Controller
 * Handles UI interactions, state management, and real-time updates.
 */

let currentUser = null;
let currentBooks = [];
let myRentals = [];
let myFines = [];
let allAdminRentals = [];
let searchDebounceTimer = null;

// ===================================================================
// INITIALIZATION
// ===================================================================

document.addEventListener('DOMContentLoaded', async () => {
  initDropdowns();
  checkExistingSession();
  await loadCatalog();
  pingAllServices();

  // Polling service pings every 30 seconds
  setInterval(pingAllServices, 30000);
});

function initDropdowns() {
  const demoBtn = document.getElementById('demo-dropdown-btn');
  const demoMenu = document.getElementById('demo-dropdown-menu');

  if (demoBtn && demoMenu) {
    demoBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      demoMenu.classList.toggle('show');
    });

    document.addEventListener('click', () => {
      demoMenu.classList.remove('show');
    });
  }
}

// ===================================================================
// AUTHENTICATION & SESSION MANAGEMENT
// ===================================================================

function checkExistingSession() {
  const token = Api.getToken();
  const user = Api.getUser();

  if (token && user) {
    currentUser = user;
    renderUserUI(user);
    loadUserContextData();
  } else {
    renderGuestUI();
  }
}

function renderUserUI(user) {
  document.getElementById('auth-buttons-group').classList.add('hidden');
  const profileGroup = document.getElementById('user-profile-group');
  profileGroup.classList.remove('hidden');

  document.getElementById('current-user-name').textContent = user.fullName || user.username;
  document.getElementById('current-user-role').textContent = user.role || 'STUDENT';
  document.getElementById('user-avatar-initial').textContent = (user.fullName || user.username).charAt(0).toUpperCase();

  // Show/Hide Admin Tab & Controls
  const isStaff = user.role === 'ADMIN' || user.role === 'LIBRARIAN';
  const adminTabBtn = document.getElementById('admin-tab-btn');
  const addBookBtn = document.getElementById('add-book-btn');

  if (isStaff) {
    adminTabBtn.classList.remove('hidden');
    addBookBtn.classList.remove('hidden');
  } else {
    adminTabBtn.classList.add('hidden');
    addBookBtn.classList.add('hidden');
  }
}

function renderGuestUI() {
  currentUser = null;
  document.getElementById('auth-buttons-group').classList.remove('hidden');
  document.getElementById('user-profile-group').classList.add('hidden');
  document.getElementById('admin-tab-btn').classList.add('hidden');
  document.getElementById('add-book-btn').classList.add('hidden');

  document.getElementById('active-loans-count').textContent = '0';
  document.getElementById('unpaid-fines-count').textContent = '0';
}

async function quickLogin(username, password) {
  const demoMenu = document.getElementById('demo-dropdown-menu');
  if (demoMenu) demoMenu.classList.remove('show');

  showToast(`Authenticating ${username}...`, 'info');
  try {
    const res = await Api.auth.login(username, password);
    handleAuthSuccess(res);
  } catch (err) {
    showToast(`Authentication failed: ${err.message}`, 'error');
  }
}

async function handleLogin(e) {
  e.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;
  const btn = document.getElementById('login-submit-btn');

  btn.disabled = true;
  btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Signing in...';

  try {
    const res = await Api.auth.login(username, password);
    closeLoginModal();
    handleAuthSuccess(res);
  } catch (err) {
    showToast(`Login failed: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<i class="fa-solid fa-arrow-right-to-bracket"></i> Sign In';
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const username = document.getElementById('reg-username').value.trim();
  const fullName = document.getElementById('reg-fullname').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const studentId = document.getElementById('reg-studentid').value.trim();
  const password = document.getElementById('reg-password').value;
  const role = document.getElementById('reg-role').value;
  const btn = document.getElementById('reg-submit-btn');

  btn.disabled = true;
  btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Registering...';

  try {
    const res = await Api.auth.register({ username, fullName, email, studentId, password, role });
    closeLoginModal();
    handleAuthSuccess(res);
  } catch (err) {
    showToast(`Registration failed: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<i class="fa-solid fa-user-plus"></i> Create Account';
  }
}

function handleAuthSuccess(res) {
  Api.setToken(res.token);
  const user = {
    id: res.id,
    username: res.username,
    fullName: res.fullName,
    email: res.email,
    studentId: res.studentId,
    role: res.role
  };
  Api.setUser(user);
  currentUser = user;
  renderUserUI(user);
  loadUserContextData();
  showToast(`Welcome back, ${user.fullName}!`, 'success');
}

function logoutUser() {
  Api.clearSession();
  renderGuestUI();
  showToast('You have signed out successfully.', 'info');
  switchTab('catalog-tab');
}

function loadUserContextData() {
  loadMyRentals();
  loadMyFines();
  if (currentUser && (currentUser.role === 'ADMIN' || currentUser.role === 'LIBRARIAN')) {
    loadAdminData();
  }
}

// ===================================================================
// TAB NAVIGATION
// ===================================================================

function switchTab(tabId) {
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.classList.toggle('active', tab.getAttribute('data-tab') === tabId);
  });

  document.querySelectorAll('.tab-pane').forEach(pane => {
    pane.classList.toggle('active', pane.id === tabId);
  });

  // Refresh tab-specific data on activation
  if (tabId === 'loans-tab' && currentUser) {
    loadMyRentals();
  } else if (tabId === 'fines-tab' && currentUser) {
    loadMyFines();
  } else if (tabId === 'admin-tab' && currentUser) {
    loadAdminData();
  } else if (tabId === 'architecture-tab') {
    pingAllServices();
  }
}

// ===================================================================
// CATALOG MANAGEMENT
// ===================================================================

async function loadCatalog() {
  try {
    const search = document.getElementById('catalog-search-input').value.trim();
    const category = document.getElementById('catalog-category-select').value;
    const branch = document.getElementById('catalog-branch-select').value;

    const books = await Api.books.getAll(search, category, branch);
    currentBooks = books || [];
    renderCatalogGrid(currentBooks);
    updateCatalogMetrics(currentBooks);
  } catch (err) {
    document.getElementById('books-grid-container').innerHTML = `
      <div class="col-span-full text-center p-4 glass-panel text-danger">
        <i class="fa-solid fa-triangle-exclamation fa-2x mb-2"></i>
        <p>Could not connect to API Gateway (:8080) or Book Service (:8082).</p>
        <small class="text-muted">Ensure all microservices are running. Click "Microservices Topology" to check status.</small>
      </div>
    `;
    document.getElementById('catalog-results-count').textContent = '0 books loaded';
  }
}

function renderCatalogGrid(books) {
  const container = document.getElementById('books-grid-container');
  const countLabel = document.getElementById('catalog-results-count');

  if (!books || books.length === 0) {
    container.innerHTML = `
      <div class="text-center p-4 glass-panel" style="grid-column: 1 / -1;">
        <i class="fa-solid fa-book-open-reader fa-2x text-muted mb-2"></i>
        <p class="text-muted">No academic books found matching your filter criteria.</p>
      </div>
    `;
    countLabel.textContent = '0 titles found';
    return;
  }

  countLabel.textContent = `Showing ${books.length} academic title${books.length > 1 ? 's' : ''}`;

  container.innerHTML = books.map(book => {
    const isAvailable = book.availableCopies > 0;
    const stockClass = isAvailable ? 'in-stock' : 'out-of-stock';
    const stockIcon = isAvailable ? 'fa-check' : 'fa-xmark';
    const stockText = isAvailable 
      ? `${book.availableCopies} of ${book.totalCopies} Available` 
      : 'All Copies Currently Checked Out';

    return `
      <div class="glass-panel book-card">
        <div>
          <div class="book-card-header">
            <span class="book-category-tag">${escapeHtml(book.category)}</span>
            <span class="book-branch-tag"><i class="fa-solid fa-location-dot"></i> ${escapeHtml(book.branch)}</span>
          </div>
          <h3 class="book-title">${escapeHtml(book.title)}</h3>
          <div class="book-author"><i class="fa-solid fa-pen-nib mr-1"></i> ${escapeHtml(book.author)}</div>
        </div>

        <div>
          <div class="book-meta-row">
            <span class="book-isbn">ISBN: ${escapeHtml(book.isbn)}</span>
            <span class="stock-indicator ${stockClass}">
              <i class="fa-solid ${stockIcon}"></i> ${stockText}
            </span>
          </div>

          <div class="book-card-actions">
            <button class="btn btn-primary btn-sm btn-block" 
                    ${!isAvailable ? 'disabled' : ''} 
                    onclick="handleBorrowBook(${book.id}, '${escapeHtml(book.title)}')">
              <i class="fa-solid fa-hand-holding-hand"></i> ${isAvailable ? 'Borrow Book' : 'Out of Stock'}
            </button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function updateCatalogMetrics(books) {
  let totalTitles = books.length;
  let totalCopies = 0;
  let availableCopies = 0;

  books.forEach(b => {
    totalCopies += (b.totalCopies || 0);
    availableCopies += (b.availableCopies || 0);
  });

  const issuedCopies = totalCopies - availableCopies;

  document.getElementById('metric-total-titles').textContent = totalTitles;
  document.getElementById('metric-available-copies').textContent = availableCopies;
  document.getElementById('metric-issued-copies').textContent = Math.max(0, issuedCopies);
}

function debounceSearch() {
  clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(loadCatalog, 300);
}

function filterCatalog() {
  loadCatalog();
}

function resetFilters() {
  document.getElementById('catalog-search-input').value = '';
  document.getElementById('catalog-category-select').value = '';
  document.getElementById('catalog-branch-select').value = '';
  loadCatalog();
}

// ===================================================================
// BORROWING & CIRCULATION (RENTAL SERVICE)
// ===================================================================

async function handleBorrowBook(bookId, bookTitle) {
  if (!currentUser) {
    showToast('Please sign in or choose a Quick Demo Account to borrow books.', 'warning');
    openLoginModal();
    return;
  }

  showToast(`Requesting loan for "${bookTitle}"...`, 'info');

  try {
    const loan = await Api.rentals.borrow(bookId, 14);
    showToast(`Book successfully borrowed! Due on: ${loan.dueDate}`, 'success');
    await loadCatalog(); // Refresh available count
    await loadMyRentals();
  } catch (err) {
    // Check for Duplicate Borrow Protection (HTTP 409)
    if (err.status === 409) {
      showToast(`DUPLICATE BORROW PROHIBITED: ${err.message}`, 'error');
    } else {
      showToast(`Borrow failed: ${err.message}`, 'error');
    }
  }
}

async function loadMyRentals() {
  if (!currentUser) return;

  try {
    const rentals = await Api.rentals.getMyRentals();
    myRentals = rentals || [];
    renderRentalsView(myRentals);
  } catch (err) {
    console.error('Failed to load my rentals:', err);
  }
}

function renderRentalsView(rentals) {
  const activeLoans = rentals.filter(r => r.status === 'ISSUED' || r.status === 'OVERDUE');
  const returnedLoans = rentals.filter(r => r.status === 'RETURNED');

  // Update nav badge
  document.getElementById('active-loans-count').textContent = activeLoans.length;

  // Render Active Loans Cards
  const activeContainer = document.getElementById('active-loans-container');
  if (activeLoans.length === 0) {
    activeContainer.innerHTML = `
      <div class="text-center p-4 glass-panel" style="grid-column: 1 / -1;">
        <i class="fa-solid fa-circle-check fa-2x text-success mb-2"></i>
        <p class="text-muted">You have no active loans checked out. Head over to the Book Catalog to borrow titles!</p>
      </div>
    `;
  } else {
    activeContainer.innerHTML = activeLoans.map(loan => {
      const isOverdue = loan.overdue;
      const statusBadgeClass = isOverdue ? 'badge-danger' : 'badge-info';
      const statusText = isOverdue ? 'OVERDUE' : 'ACTIVE LOAN';

      return `
        <div class="glass-panel loan-card ${isOverdue ? 'is-overdue' : ''}">
          <div class="loan-card-top">
            <span class="loan-id">Loan #${loan.id}</span>
            <span class="badge ${statusBadgeClass}">${statusText}</span>
          </div>

          <h3 class="book-title">${escapeHtml(loan.bookTitle)}</h3>
          
          <div class="loan-dates-row">
            <div class="date-box">
              <span class="label">Issue Date</span>
              <div class="value">${loan.issueDate}</div>
            </div>
            <div class="date-box">
              <span class="label">Due Date</span>
              <div class="value text-${isOverdue ? 'danger' : 'primary'}">${loan.dueDate}</div>
            </div>
          </div>

          ${isOverdue ? `
            <div class="overdue-alert-ribbon">
              <i class="fa-solid fa-triangle-exclamation"></i>
              <span>Book is overdue! Late penalty will be calculated upon return.</span>
            </div>
          ` : ''}

          <div class="loan-actions">
            <button class="btn btn-primary btn-sm btn-block" onclick="handleReturnBook(${loan.id})">
              <i class="fa-solid fa-arrow-turn-down-left"></i> Return Book
            </button>
            <button class="btn btn-outline btn-sm" title="Simulate 5 days overdue for demo grading" onclick="handleSimulateOverdue(${loan.id})">
              <i class="fa-solid fa-bolt text-warning"></i> Simulate Overdue
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  // Render Returned Loans Table
  const tableBody = document.getElementById('returned-history-table-body');
  if (returnedLoans.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">No returned history found.</td></tr>`;
  } else {
    tableBody.innerHTML = returnedLoans.map(loan => {
      const fineCol = loan.fineId 
        ? `<span class="badge badge-danger">Fine #${loan.fineId}</span>`
        : `<span class="badge badge-success">No Penalty</span>`;

      return `
        <tr>
          <td>#${loan.id}</td>
          <td><strong>${escapeHtml(loan.bookTitle)}</strong></td>
          <td>${loan.issueDate}</td>
          <td>${loan.dueDate}</td>
          <td>${loan.returnDate || '-'}</td>
          <td><span class="badge badge-success">RETURNED</span></td>
          <td>${fineCol}</td>
        </tr>
      `;
    }).join('');
  }
}

async function handleReturnBook(rentalId) {
  showToast('Processing return and restocking book inventory...', 'info');
  try {
    const res = await Api.rentals.returnBook(rentalId);
    if (res.fineId) {
      showToast(`Book returned! Late fee assessed: ₹${res.fineAmount} (${res.overdueDays} days overdue).`, 'warning');
    } else {
      showToast('Book returned successfully on time! Inventory updated.', 'success');
    }
    await loadCatalog();
    await loadMyRentals();
    await loadMyFines();
  } catch (err) {
    showToast(`Return failed: ${err.message}`, 'error');
  }
}

async function handleSimulateOverdue(rentalId) {
  showToast('Fast-forwarding due date by 5 days past today...', 'info');
  try {
    await Api.rentals.simulateOverdue(rentalId, 5);
    showToast('Due date updated to 5 days ago! Loan marked as OVERDUE.', 'warning');
    await loadMyRentals();
  } catch (err) {
    showToast(`Simulation failed: ${err.message}`, 'error');
  }
}

// ===================================================================
// FINES & PENALTIES (FINE SERVICE)
// ===================================================================

async function loadMyFines() {
  if (!currentUser) return;

  try {
    const fines = await Api.fines.getMyFines();
    myFines = fines || [];
    renderFinesView(myFines);
  } catch (err) {
    console.error('Failed to load fines:', err);
  }
}

function renderFinesView(fines) {
  let unpaidTotal = 0;
  let paidTotal = 0;
  let unpaidCount = 0;

  fines.forEach(f => {
    if (f.status === 'UNPAID') {
      unpaidTotal += f.amount;
      unpaidCount++;
    } else {
      paidTotal += f.amount;
    }
  });

  document.getElementById('metric-unpaid-fines').textContent = `₹${unpaidTotal.toFixed(2)}`;
  document.getElementById('metric-paid-fines').textContent = `₹${paidTotal.toFixed(2)}`;
  document.getElementById('metric-total-fines-count').textContent = fines.length;
  document.getElementById('unpaid-fines-count').textContent = unpaidCount;

  const tableBody = document.getElementById('fines-table-body');
  if (fines.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="9" class="text-center text-muted">No fines or late fees on record. All clear!</td></tr>`;
    return;
  }

  tableBody.innerHTML = fines.map(fine => {
    const isUnpaid = fine.status === 'UNPAID';
    const statusBadge = isUnpaid 
      ? `<span class="badge badge-danger">UNPAID</span>`
      : `<span class="badge badge-success">PAID</span>`;

    const actionBtn = isUnpaid
      ? `<button class="btn btn-success btn-xs" onclick="handlePayFine(${fine.id})"><i class="fa-solid fa-wallet"></i> Pay ₹${fine.amount}</button>`
      : `<span class="text-muted"><i class="fa-solid fa-check"></i> Settled</span>`;

    return `
      <tr>
        <td>#${fine.id}</td>
        <td>Loan #${fine.rentalId}</td>
        <td>${escapeHtml(fine.studentId)}</td>
        <td><strong>${fine.overdueDays} days</strong></td>
        <td>₹${fine.dailyRate || 10}/day</td>
        <td class="text-${isUnpaid ? 'danger' : 'success'}"><strong>₹${fine.amount.toFixed(2)}</strong></td>
        <td>${statusBadge}</td>
        <td>${fine.createdAt ? fine.createdAt.substring(0, 10) : '-'}</td>
        <td>${actionBtn}</td>
      </tr>
    `;
  }).join('');
}

async function handlePayFine(fineId) {
  showToast(`Processing settlement for fine #${fineId}...`, 'info');
  try {
    await Api.fines.pay(fineId);
    showToast(`Fine #${fineId} settled successfully!`, 'success');
    await loadMyFines();
  } catch (err) {
    showToast(`Payment failed: ${err.message}`, 'error');
  }
}

// ===================================================================
// LIBRARIAN & ADMIN OPERATIONS
// ===================================================================

async function loadAdminData() {
  if (!currentUser || (currentUser.role !== 'ADMIN' && currentUser.role !== 'LIBRARIAN')) return;

  try {
    // 1. Load All Institutional Rentals
    allAdminRentals = await Api.rentals.getAll() || [];
    renderAdminRentals(allAdminRentals);

    // 2. Load Overdue Loans Audit
    const overdueRentals = await Api.rentals.getOverdue() || [];
    renderAdminOverdue(overdueRentals);

    // 3. Load All Master Fines
    const allFines = await Api.fines.getAll() || [];
    renderAdminFines(allFines);
  } catch (err) {
    console.error('Failed to load admin circulation data:', err);
  }
}

function renderAdminOverdue(overdueList) {
  document.getElementById('admin-overdue-count').textContent = `${overdueList.length} Overdue`;
  const tbody = document.getElementById('admin-overdue-table-body');

  if (overdueList.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No overdue books right now. All circulation healthy!</td></tr>`;
    return;
  }

  tbody.innerHTML = overdueList.map(loan => `
    <tr>
      <td>#${loan.id}</td>
      <td><strong>${escapeHtml(loan.studentId)}</strong></td>
      <td>${escapeHtml(loan.studentUsername)}</td>
      <td>${escapeHtml(loan.bookTitle)}</td>
      <td class="text-danger">${loan.dueDate}</td>
      <td><span class="badge badge-danger">Overdue</span></td>
    </tr>
  `).join('');
}

function renderAdminRentals(rentals) {
  const tbody = document.getElementById('admin-all-rentals-table-body');
  if (rentals.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No circulation records found.</td></tr>`;
    return;
  }

  tbody.innerHTML = rentals.map(loan => {
    let statusBadge = `<span class="badge badge-info">${loan.status}</span>`;
    if (loan.status === 'OVERDUE') statusBadge = `<span class="badge badge-danger">OVERDUE</span>`;
    if (loan.status === 'RETURNED') statusBadge = `<span class="badge badge-success">RETURNED</span>`;

    return `
      <tr>
        <td>#${loan.id}</td>
        <td>${escapeHtml(loan.studentId)}</td>
        <td>${escapeHtml(loan.studentUsername)}</td>
        <td>${escapeHtml(loan.bookTitle)}</td>
        <td>${loan.issueDate}</td>
        <td>${loan.dueDate}</td>
        <td>${statusBadge}</td>
        <td>${loan.fineId ? `#${loan.fineId}` : '-'}</td>
      </tr>
    `;
  }).join('');
}

function filterAdminRentals(filter) {
  document.querySelectorAll('.admin-card .btn-group button').forEach(b => b.classList.remove('active'));
  event.target.classList.add('active');

  if (filter === 'ALL') {
    renderAdminRentals(allAdminRentals);
  } else {
    renderAdminRentals(allAdminRentals.filter(r => r.status === filter));
  }
}

function renderAdminFines(fines) {
  const tbody = document.getElementById('admin-all-fines-table-body');
  if (fines.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No fine records found.</td></tr>`;
    return;
  }

  tbody.innerHTML = fines.map(fine => {
    const isUnpaid = fine.status === 'UNPAID';
    const statusBadge = isUnpaid 
      ? `<span class="badge badge-danger">UNPAID</span>`
      : `<span class="badge badge-success">PAID</span>`;

    return `
      <tr>
        <td>#${fine.id}</td>
        <td>${escapeHtml(fine.studentId)} (${escapeHtml(fine.studentUsername)})</td>
        <td>Loan #${fine.rentalId}</td>
        <td>${fine.overdueDays} days</td>
        <td>₹${fine.amount.toFixed(2)}</td>
        <td>${statusBadge}</td>
        <td>${fine.createdAt ? fine.createdAt.substring(0, 16).replace('T', ' ') : '-'}</td>
        <td>${fine.paidAt ? fine.paidAt.substring(0, 16).replace('T', ' ') : '-'}</td>
      </tr>
    `;
  }).join('');
}

async function handleAddBook(e) {
  e.preventDefault();
  const title = document.getElementById('book-title').value.trim();
  const author = document.getElementById('book-author').value.trim();
  const isbn = document.getElementById('book-isbn').value.trim();
  const category = document.getElementById('book-category').value.trim();
  const totalCopies = parseInt(document.getElementById('book-copies').value, 10);
  const branch = document.getElementById('book-branch').value;
  const btn = document.getElementById('save-book-btn');

  btn.disabled = true;
  btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Saving...';

  try {
    await Api.books.createBook({ title, author, isbn, category, totalCopies, branch });
    closeAddBookModal();
    showToast(`Book "${title}" added to ${branch}!`, 'success');
    await loadCatalog();
  } catch (err) {
    showToast(`Failed to add book: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Save Book to Inventory';
  }
}

// ===================================================================
// MICROSERVICES TOPOLOGY PING ENGINE
// ===================================================================

async function pingAllServices() {
  // 1. Gateway Ping (:8080)
  const isGatewayUp = await Api.system.checkGateway();
  updateServiceCard('gateway', isGatewayUp);
  updateHeaderIndicator('gateway-indicator', isGatewayUp);

  // 2. Direct pings to all 5 microservices
  const services = [
    { id: 'eureka', port: 8761 },
    { id: 'auth', port: 8081 },
    { id: 'book', port: 8082 },
    { id: 'rental', port: 8083 },
    { id: 'fine', port: 8084 }
  ];

  for (const svc of services) {
    const isUp = await Api.system.checkServiceDirect(svc.port);
    updateServiceCard(svc.id, isUp);
    if (svc.id === 'eureka') {
      updateHeaderIndicator('eureka-indicator', isUp);
    }
  }
}

function updateServiceCard(id, isUp) {
  const el = document.getElementById(`ping-${id}`);
  if (!el) return;

  if (isUp) {
    el.className = 'status-indicator online';
    el.innerHTML = '<i class="fa-solid fa-circle-check"></i> Online & Healthy';
  } else {
    el.className = 'status-indicator offline';
    el.innerHTML = '<i class="fa-solid fa-circle-xmark"></i> Offline / Unreachable';
  }
}

function updateHeaderIndicator(id, isUp) {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = `status-pill ${isUp ? 'online' : 'offline'}`;
}

// ===================================================================
// MODAL & TOAST UTILITIES
// ===================================================================

function openLoginModal() {
  document.getElementById('login-modal').classList.remove('hidden');
}

function closeLoginModal() {
  document.getElementById('login-modal').classList.add('hidden');
}

function toggleAuthMode() {
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');
  const modalTitle = document.getElementById('auth-modal-title');

  if (loginForm.classList.contains('hidden')) {
    loginForm.classList.remove('hidden');
    registerForm.classList.add('hidden');
    modalTitle.innerHTML = '<i class="fa-solid fa-right-to-bracket text-primary mr-1"></i> Sign In to Bibliotech';
  } else {
    loginForm.classList.add('hidden');
    registerForm.classList.remove('hidden');
    modalTitle.innerHTML = '<i class="fa-solid fa-user-plus text-primary mr-1"></i> Student Registration';
  }
}

function openAddBookModal() {
  document.getElementById('add-book-modal').classList.remove('hidden');
}

function closeAddBookModal() {
  document.getElementById('add-book-modal').classList.add('hidden');
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let icon = 'fa-circle-info';
  let title = 'Information';
  if (type === 'success') { icon = 'fa-circle-check'; title = 'Success'; }
  if (type === 'error') { icon = 'fa-circle-exclamation'; title = 'Notice'; }
  if (type === 'warning') { icon = 'fa-triangle-exclamation'; title = 'Attention'; }

  toast.innerHTML = `
    <div class="toast-icon"><i class="fa-solid ${icon}"></i></div>
    <div class="toast-message">
      <strong>${title}</strong>
      <p>${escapeHtml(message)}</p>
    </div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(40px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4500);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
