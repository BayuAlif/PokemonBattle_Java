async function registerAccount(usernameInput, passwordInput) {
    const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: usernameInput, password: passwordInput })
    });
    const data = await response.json();
    alert(data.message);
}


// HANDLER UNTUK REGISTER TRAINER
async function handleRegister(event) {
    event.preventDefault();

    const username = document.getElementById('reg-username').value;
    const password = document.getElementById('reg-password').value;
    const passwordConfirm = document.getElementById('reg-pass-confirm').value;

    if (password !== passwordConfirm) {
        alert("Konfirmasi password tidak cocok!");
        return;
    }

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();
        alert(data.message);

        if (data.success) {
            window.location.href = '/login'; 
        }
    } catch (error) {
        console.error('Error Register:', error);
        alert('Gagal terhubung ke database registrasi.');
    }
}

// HANDLER UNTUK LOGIN TRAINER
async function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (data.success) {
            alert(data.message);
            localStorage.setItem('playerName', data.username);
            window.location.href = '/home';
        } else {
            alert(data.message);
        }
    } catch (error) {
        console.error('Error Login:', error);
        alert('Gagal memproses verifikasi login.');
    }
}

