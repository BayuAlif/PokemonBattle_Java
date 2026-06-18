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

//Handle untuk dashboard
async function loadDashboardStats() {
    try {
        const response = await fetch('/api/user/dashboard-stats');
        const data = await response.json();

        if (data.success) {
            // Ambil elemen HTML berdasarkan ID
            const badgeNameEl = document.getElementById('dash-badge-username');
            const avatarEl = document.getElementById('dash-avatar');
            const nameEl = document.getElementById('dash-username');
            const pokemonEl = document.getElementById('dash-pokemon');
            const winsEl = document.getElementById('dash-wins');
            const lossesEl = document.getElementById('dash-losses');
            const itemsEl = document.getElementById('dash-items');
            
            // Elemen teks deskripsi bawah
            const descPokemonEl = document.getElementById('dash-desc-pokemon');
            const descItemsEl = document.getElementById('dash-desc-items');

            // Suntikkan data asli dari database SQLite ke UI
            if (badgeNameEl) badgeNameEl.innerText = `⭐ ${data.username}`;
            if (nameEl) nameEl.innerText = data.username;
            
            // Buat inisial avatar dinamis berdasarkan huruf pertama nama user
            if (avatarEl && data.username) {
                avatarEl.innerText = data.username.charAt(0).toUpperCase();
            }

            if (pokemonEl) pokemonEl.innerText = data.totalPokemon;
            if (winsEl) winsEl.innerText = data.wins;
            if (lossesEl) lossesEl.innerText = data.losses;
            if (itemsEl) itemsEl.innerText = data.totalItems;

            // Perbarui deskripsi quick action secara dinamis
            if (descPokemonEl) descPokemonEl.innerText = `Kelola ${data.totalPokemon} Pokémon dalam koleksimu`;
            if (descItemsEl) descItemsEl.innerText = `${data.totalItems} item tersedia untuk digunakan`;
        } else {
            console.error("Gagal memuat stats:", data.message);
        }
    } catch (error) {
        console.error("Error saat melakukan fetch dashboard stats:", error);
    }
}

// LOGIKA PROSES LOGOUT
async function handleLogout() {
    if (confirm("Apakah kamu yakin ingin keluar dari permainan?")) {
        try {
            const response = await fetch('/api/auth/logout', { method: 'POST' });
            const data = await response.json();
            if (data.success) {
                localStorage.removeItem('playerName');
                window.location.href = '/login';
            }
        } catch (error) {
            console.error("Error Logout:", error);
            alert("Gagal terhubung untuk proses logout.");
        }
    }
}

// Jalankan otomatis ketika DOM HTML selesai dimuat
document.addEventListener("DOMContentLoaded", () => {
    if (document.body.dataset.page === 'home') {
        loadDashboardStats();
    }
});

//Handle untuk menampilkan collection
async function fetchCollectionData() {
    const grid = document.getElementById('collection-grid');
    if (!grid) return;

    try {
        const response = await fetch('/api/user/collection');
        if (!response.ok) throw new Error('Gagal mengambil data koleksi');
        const pokemons = await response.json();

        grid.innerHTML = '';

        if (pokemons.length === 0) {
            grid.innerHTML = '<p style="color: var(--text3); grid-column: 1/-1; text-align: center;">Koleksi kamu masih kosong. Ayo tangkap Pokémon!</p>';
            return;
        }

        pokemons.forEach(pokemon => {
            const card = document.createElement('div');
            
            // Logika lencana kelangkaan dan border emas khusus LEGENDARY
            const isLegendary = pokemon.rarity === 'LEGENDARY';
            const cardBorder = isLegendary ? 'border: 2px solid #f5d44f; box-shadow: 0 0 15px rgba(245, 212, 79, 0.3);' : 'border: 1px solid var(--accent, #444);';
            
            card.style.cssText = `background: rgba(30, 30, 30, 0.6); ${cardBorder} border-radius: 12px; padding: 20px; text-align: center; position: relative;`;

            let icon = 'ti-pokeball';
            let typeColor = '#777';

            if (pokemon.type === 'FIRE') { icon = 'ti-flame'; typeColor = '#ff5e5e'; }
            else if (pokemon.type === 'WATER') { icon = 'ti-droplet'; typeColor = '#5e81ff'; }
            else if (pokemon.type === 'GRASS') { icon = 'ti-leaf'; typeColor = '#5eff81'; }
            else if (pokemon.type === 'ELECTRIC') { icon = 'ti-bolt'; typeColor = '#f5d44f'; }
            else if (pokemon.type === 'ICE') { icon = 'ti-snowflake'; typeColor = '#a8e4ff'; }
            else if (pokemon.type === 'POISON') { icon = 'ti-skull'; typeColor = '#b75eff'; }

            const rarityBadge = isLegendary 
                ? `<div style="position: absolute; top: 15px; left: 15px; background: #f5d44f; color: #111; font-size: 9px; font-weight: 900; padding: 2px 6px; border-radius: 4px; letter-spacing: 0.5px;">⭐ LEGENDARY</div>` 
                : '';

            card.innerHTML = `
                ${rarityBadge}
                <div style="position: absolute; top: 15px; right: 15px; background: ${typeColor}; color: white; font-size: 10px; font-weight: bold; padding: 4px 8px; border-radius: 4px;">
                    ${pokemon.type}
                </div>
                <i class="ti ${icon}" style="font-size: 45px; color: ${typeColor}; margin-top: 15px; margin-bottom: 10px; display: block;"></i>
                <h3 style="margin: 0 0 10px 0; color: white; font-size: 22px; font-weight: bold;">${pokemon.name}</h3>
                
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; text-align: left; background: #222; padding: 12px; border-radius: 8px;">
                    <div style="font-size: 12px; color: #aaa;">
                        <i class="ti ti-heart" style="color: #ff5e5e;"></i> HP: <span style="color: white; font-weight: bold;">${pokemon.hp}/${pokemon.max_hp}</span>
                    </div>
                    <div style="font-size: 12px; color: #aaa;">
                        <i class="ti ti-sword" style="color: #5e81ff;"></i> Atk: <span style="color: white; font-weight: bold;">${pokemon.attack}</span>
                    </div>
                    <div style="font-size: 12px; color: #aaa;">
                        <i class="ti ti-shield" style="color: #5eff81;"></i> Def: <span style="color: white; font-weight: bold;">${pokemon.defense}</span>
                    </div>
                    <div style="font-size: 12px; color: #aaa;">
                        <i class="ti ti-bolt" style="color: #f5d44f;"></i> Spd: <span style="color: white; font-weight: bold;">${pokemon.speed}</span>
                    </div>
                </div>
            `;
            grid.appendChild(card);
        });

    } catch (error) {
        console.error('Error memuat koleksi:', error);
        grid.innerHTML = '<p style="color: #ff5e5e; grid-column: 1/-1; text-align: center;">Terjadi kesalahan saat memuat data koleksi Pokémon.</p>';
    }
}

//Handle untuk menampilkan inventory
async function fetchInventoryData() {
    const grid = document.getElementById('inventory-grid');
    if (!grid) return;

    try {
        const response = await fetch('/api/user/inventory');
        if (!response.ok) throw new Error('Gagal mengambil data inventory');
        const items = await response.json();

        grid.innerHTML = '';

        if (items.length === 0) {
            grid.innerHTML = '<p style="color: var(--text3); grid-column: 1/-1; text-align: center;">Tas kamu masih kosong.</p>';
            return;
        }

        items.forEach(item => {
            const card = document.createElement('div');
            card.style.cssText = 'background: rgba(30, 30, 30, 0.6); border: 1px solid var(--accent, #444); border-radius: 12px; padding: 20px; position: relative;';

            // Mengatur Icon & Warna berdasarkan Tipe Item (HEAL, REVIVE, CURE)
            let icon = 'ti-package';
            let iconColor = '#aaa';
            let effectLabel = `Efek: +${item.effect_value}`;

            if(item.type === 'HEAL') { 
                icon = 'ti-first-aid-kit'; iconColor = '#ff5e5e'; 
            } else if(item.type === 'REVIVE') { 
                icon = 'ti-heart-rate-monitor'; iconColor = '#f5d44f'; 
            } else if(item.type === 'CURE') { 
                icon = 'ti-vaccine'; iconColor = '#5eff81'; 
                effectLabel = 'Menyembuhkan Status'; 
            }

            card.innerHTML = `
                <div style="position: absolute; top: 15px; right: 15px; background: rgba(255,255,255,0.1); color: white; font-size: 14px; font-weight: bold; padding: 4px 10px; border-radius: 6px;">
                    x${item.quantity}
                </div>
                <i class="ti ${icon}" style="font-size: 40px; color: ${iconColor}; margin-bottom: 15px; display: block;"></i>
                <h3 style="margin: 0 0 5px 0; color: white; font-size: 20px;">${item.name}</h3>
                <p style="margin: 0 0 15px 0; color: #aaa; font-size: 13px; line-height: 1.4; height: 36px; overflow: hidden;">${item.description}</p>
                <div style="background: #222; padding: 8px 12px; border-radius: 6px; font-size: 12px; color: ${iconColor}; display: inline-block;">
                    ${effectLabel}
                </div>
            `;
            grid.appendChild(card);
        });

    } catch (error) {
        console.error('Error memuat inventory:', error);
        grid.innerHTML = '<p style="color: #ff5e5e; grid-column: 1/-1; text-align: center;">Terjadi kesalahan saat memuat isi tas.</p>';
    }
}