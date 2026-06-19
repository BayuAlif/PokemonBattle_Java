// =========================================================================
// 1. GLOBAL UTILITIES & NAVIGATION
// =========================================================================
let globalPokemonLeft = 3; // Melacak sisa slot nyawa tim saat pertempuran aktif
let catchAttempts = 0;     // Melacak jumlah lemparan bola fase reward (Maksimal 3)
let globalSwapLeft = 3; // Melacak sisa kuota ganti acak di UI

function goTo(page) { 
    window.location.href = '/' + page; 
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

function toggleMenu(menuId) {
    const mainMenu = document.getElementById('menu-main');
    const skillsMenu = document.getElementById('menu-skills');
    const itemsMenu = document.getElementById('menu-items');

    if (mainMenu) mainMenu.style.display = 'none';
    if (skillsMenu) skillsMenu.style.display = 'none';
    if (itemsMenu) itemsMenu.style.display = 'none';

    const targetMenu = document.getElementById(menuId);
    if (targetMenu) targetMenu.style.display = 'grid';
    
    if (menuId === 'menu-items') loadItemList();
}

function addLog(text, color = "white") {
    const logBox = document.getElementById('battle-log');
    if (!logBox) return;
    const time = new Date().toLocaleTimeString('id-ID', { hour12: false });
    logBox.innerHTML += `<p style="color: ${color};"><span style="color:#64748b; font-size:11px;">[${time}]</span> ${text}</p>`;
    logBox.scrollTop = logBox.scrollHeight; 
}

function lockActionButtons(locked) {
    const buttons = document.querySelectorAll('.btn-action');
    buttons.forEach(btn => locked ? btn.classList.add('disabled') : btn.classList.remove('disabled'));
}

// =========================================================================
// 2. LOGIKA AUTENTIKASI (LOGIN, REGISTER & LOGOUT)
// =========================================================================
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
        if (data.success) window.location.href = '/login'; 
    } catch (error) {
        console.error('Error Register:', error);
        alert('Gagal terhubung ke database registrasi.');
    }
}

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

// =========================================================================
// 3. LOGIKA DASHBOARD HOME STATS
// =========================================================================
async function loadDashboardStats() {
    try {
        const response = await fetch('/api/user/dashboard-stats');
        const data = await response.json();

        if (data.success) {
            const badgeNameEl = document.getElementById('dash-badge-username');
            const avatarEl = document.getElementById('dash-avatar');
            const nameEl = document.getElementById('dash-username');
            const pokemonEl = document.getElementById('dash-pokemon');
            const winsEl = document.getElementById('dash-wins');
            const lossesEl = document.getElementById('dash-losses');
            const itemsEl = document.getElementById('dash-items');
            const descPokemonEl = document.getElementById('dash-desc-pokemon');
            const descItemsEl = document.getElementById('dash-desc-items');

            if (badgeNameEl) badgeNameEl.innerText = `⭐ ${data.username}`;
            if (nameEl) nameEl.innerText = data.username;
            if (avatarEl && data.username) {
                avatarEl.innerText = data.username.charAt(0).toUpperCase();
            }
            if (pokemonEl) pokemonEl.innerText = data.totalPokemon;
            if (winsEl) winsEl.innerText = data.wins;
            if (lossesEl) lossesEl.innerText = data.losses;
            if (itemsEl) itemsEl.innerText = data.totalItems;

            if (descPokemonEl) descPokemonEl.innerText = `Kelola ${data.totalPokemon} Pokémon dalam koleksimu`;
            if (descItemsEl) descItemsEl.innerText = `${data.totalItems} item tersedia untuk digunakan`;
        }
    } catch (error) {
        console.error("Error memuat dashboard stats:", error);
    }
}

// =========================================================================
// 4. LOGIKA MENAMPILKAN KREASIONAL KOLEKSI POKEMON
// =========================================================================
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
            const isLegendary = pokemon.rarity === 'LEGENDARY';
            const cardBorder = isLegendary ? 'border: 2px solid #f5d44f; box-shadow: 0 0 15px rgba(245, 212, 79, 0.3);' : 'border: 1px solid var(--accent, #444);';
            card.style.cssText = `background: rgba(30, 30, 30, 0.6); ${cardBorder} border-radius: 12px; padding: 20px; text-align: center; position: relative;`;

            let icon = 'ti-pokeball'; let typeColor = '#777';
            if (pokemon.type === 'FIRE') { icon = 'ti-flame'; typeColor = '#ff5e5e'; }
            else if (pokemon.type === 'WATER') { icon = 'ti-droplet'; typeColor = '#5e81ff'; }
            else if (pokemon.type === 'GRASS') { icon = 'ti-leaf'; typeColor = '#5eff81'; }
            else if (pokemon.type === 'ELECTRIC') { icon = 'ti-bolt'; typeColor = '#f5d44f'; }
            else if (pokemon.type === 'ICE') { icon = 'ti-snowflake'; typeColor = '#a8e4ff'; }
            else if (pokemon.type === 'POISON') { icon = 'ti-skull'; typeColor = '#b75eff'; }

            const rarityBadge = isLegendary ? `<div style="position: absolute; top: 15px; left: 15px; background: #f5d44f; color: #111; font-size: 9px; font-weight: 900; padding: 2px 6px; border-radius: 4px; letter-spacing: 0.5px;">⭐ LEGENDARY</div>` : '';

            card.innerHTML = `
                ${rarityBadge}
                <div style="position: absolute; top: 15px; right: 15px; background: ${typeColor}; color: white; font-size: 10px; font-weight: bold; padding: 4px 8px; border-radius: 4px;">${pokemon.type}</div>
                <i class="ti ${icon}" style="font-size: 45px; color: ${typeColor}; margin-top: 15px; margin-bottom: 10px; display: block;"></i>
                <h3 style="margin: 0 0 10px 0; color: white; font-size: 22px; font-weight: bold;">${pokemon.name}</h3>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; text-align: left; background: #222; padding: 12px; border-radius: 8px;">
                    <div style="font-size: 12px; color: #aaa;"><i class="ti ti-heart" style="color: #ff5e5e;"></i> HP: <span style="color: white; font-weight: bold;">${pokemon.hp}/${pokemon.max_hp}</span></div>
                    <div style="font-size: 12px; color: #aaa;"><i class="ti ti-sword" style="color: #5e81ff;"></i> Atk: <span style="color: white; font-weight: bold;">${pokemon.attack}</span></div>
                    <div style="font-size: 12px; color: #aaa;"><i class="ti ti-shield" style="color: #5eff81;"></i> Def: <span style="color: white; font-weight: bold;">${pokemon.defense}</span></div>
                    <div style="font-size: 12px; color: #aaa;"><i class="ti ti-bolt" style="color: #f5d44f;"></i> Spd: <span style="color: white; font-weight: bold;">${pokemon.speed}</span></div>
                </div>`;
            grid.appendChild(card);
        });
    } catch (error) {
        console.error('Error memuat koleksi:', error);
    }
}

// =========================================================================
// 5. LOGIKA INVENTORY TAS TRAINER
// =========================================================================
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

            let icon = 'ti-package'; let iconColor = '#aaa'; let effectLabel = `Efek: +${item.effect_value}`;
            if(item.type === 'HEAL') { icon = 'ti-first-aid-kit'; iconColor = '#ff5e5e'; } 
            else if(item.type === 'REVIVE') { icon = 'ti-heart-rate-monitor'; iconColor = '#f5d44f'; } 
            else if(item.type === 'CURE') { icon = 'ti-vaccine'; iconColor = '#5eff81'; effectLabel = 'Menyembuhkan Status'; }

            card.innerHTML = `
                <div style="position: absolute; top: 15px; right: 15px; background: rgba(255,255,255,0.1); color: white; font-size: 14px; font-weight: bold; padding: 4px 10px; border-radius: 6px;">x${item.quantity}</div>
                <i class="ti ${icon}" style="font-size: 40px; color: ${iconColor}; margin-bottom: 15px; display: block;"></i>
                <h3 style="margin: 0 0 5px 0; color: white; font-size: 20px;">${item.name}</h3>
                <p style="margin: 0 0 15px 0; color: #aaa; font-size: 13px; line-height: 1.4; height: 36px; overflow: hidden;">${item.description}</p>
                <div style="background: #222; padding: 8px 12px; border-radius: 6px; font-size: 12px; color: ${iconColor}; display: inline-block;">${effectLabel}</div>`;
            grid.appendChild(card);
        });
    } catch (error) {
        console.error('Error memuat inventory:', error);
    }
}

// =========================================================================
// 6. ARENA BATTLE: CORE MECHANICS (BOSS MODE & 3-SLOT TIM NYAWA)
// =========================================================================
async function loadDeployScreen() {
    const grid = document.getElementById('deploy-grid');
    if (!grid) return;
    try {
        const response = await fetch('/api/user/collection');
        const pokemons = await response.json();
        grid.innerHTML = '';
        
        if(pokemons.length === 0) {
            grid.innerHTML = '<p style="grid-column:1/-1; text-align:center;">Kamu belum punya Pokemon untuk bertarung!</p>';
            return;
        }
        
        pokemons.forEach(p => {
            let icon = 'ti-pokeball'; let typeColor = '#777';
            if (p.type === 'FIRE') { icon = 'ti-flame'; typeColor = '#ff5e5e'; }
            else if (p.type === 'WATER') { icon = 'ti-droplet'; typeColor = '#5e81ff'; }
            else if (p.type === 'GRASS') { icon = 'ti-leaf'; typeColor = '#5eff81'; }
            else if (p.type === 'ELECTRIC') { icon = 'ti-bolt'; typeColor = '#f5d44f'; }
            else if (p.type === 'ICE') { icon = 'ti-snowflake'; typeColor = '#a8e4ff'; }
            else if (p.type === 'POISON') { icon = 'ti-skull'; typeColor = '#b75eff'; }

            const isLegendary = p.rarity === 'LEGENDARY';
            const cardBorder = isLegendary ? 'border: 2px solid #f5d44f; box-shadow: 0 0 15px rgba(245, 212, 79, 0.3);' : 'border: 1px solid var(--accent, #444);';
            const rarityBadge = isLegendary ? `<div style="position: absolute; top: 15px; left: 15px; background: #f5d44f; color: #111; font-size: 9px; font-weight: 900; padding: 2px 6px; border-radius: 4px;">⭐ LEGENDARY</div>` : '';

            grid.innerHTML += `
                <div class="deploy-card" 
                     style="background: rgba(30, 30, 30, 0.6); ${cardBorder} border-radius: 12px; padding: 20px; text-align: center; position: relative; cursor: pointer; transition: all 0.2s;"
                     onclick="handlePokemonSelection(${p.id}, '${p.type}')"
                     onmouseover="this.style.transform='translateY(-5px)'; this.style.borderColor='${typeColor}';"
                     onmouseout="this.style.transform='translateY(0)'; this.style.borderColor='${isLegendary ? '#f5d44f' : 'var(--accent)'}';">
                    ${rarityBadge}
                    <div style="position: absolute; top: 15px; right: 15px; background: ${typeColor}; color: white; font-size: 10px; font-weight: bold; padding: 4px 8px; border-radius: 4px;">${p.type}</div>
                    <i class="ti ${icon}" style="font-size: 45px; color: ${typeColor}; margin-top: 15px; margin-bottom: 10px; display: block;"></i>
                    <h3 style="margin: 0 0 10px 0; color: white; font-size: 22px;">${p.name}</h3>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; text-align: left; background: #222; padding: 12px; border-radius: 8px;">
                        <div style="font-size: 12px; color: #aaa;"><i class="ti ti-heart" style="color: #ff5e5e;"></i> HP: <span style="color: white; font-weight: bold;">${p.hp}/${p.max_hp}</span></div>
                        <div style="font-size: 12px; color: #aaa;"><i class="ti ti-sword" style="color: #5e81ff;"></i> Atk: <span style="color: white; font-weight: bold;">${p.attack}</span></div>
                        <div style="font-size: 12px; color: #aaa;"><i class="ti ti-shield" style="color: #5eff81;"></i> Def: <span style="color: white; font-weight: bold;">${p.defense}</span></div>
                        <div style="font-size: 12px; color: #aaa;"><i class="ti ti-bolt" style="color: #f5d44f;"></i> Spd: <span style="color: white; font-weight: bold;">${p.speed}</span></div>
                    </div>
                </div>`;
        });
    } catch (e) { console.error(e); }
}

function handlePokemonSelection(pokemonId, playerType) {
    const arenaContainer = document.getElementById('phase-arena');
    if (!arenaContainer) return;
    const isArenaActive = arenaContainer.style.display === 'block';
    if (!isArenaActive) {
        startBattlePhase(pokemonId, playerType);
    } else {
        sendNextPokemon(pokemonId, playerType);
    }
}

async function startBattlePhase(pokemonId, playerType) {
    document.getElementById('phase-selection').style.display = 'none';
    document.getElementById('phase-arena').style.display = 'block';
    addLog("Mengirim Pokémon pertama ke arena...", "#38bdf8");
    
    try {
        const response = await fetch('/api/battle/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerPokemonId: pokemonId })
        });
        const data = await response.json();
        
        globalPokemonLeft = data.pokemonLeft;
        globalSwapLeft = data.swapLeft !== undefined ? data.swapLeft : 3;
        
        const btnSwap = document.getElementById('btn-menu-swap');
        if (btnSwap) btnSwap.innerHTML = `<i class="ti ti-refresh"></i> Swap (${globalSwapLeft})`;

        document.getElementById('enemy-name').textContent = data.enemy.name;
        document.getElementById('player-name').textContent = data.player.name;
        
        setIconElement('sprite-player', 'icon-player', playerType);
        setIconElement('sprite-enemy', 'icon-enemy', data.enemy.type || 'NORMAL');

        updateBattleUI({
            enemyCurrentHp: data.enemyCurrentHp, enemyMaxHp: data.enemyMaxHp,
            playerCurrentHp: data.player.hp, playerMaxHp: data.player.maxHp
        });

        renderSkillButtons(data.playerMoves);
        addLog(data.message, "#5eff81");
        addLog(`Kesempatan Pokémon Tersisa: [${globalPokemonLeft}/3]`, "#ffd966");
    } catch (err) { console.error(err); }
}

async function sendNextPokemon(pokemonId, playerType) {
    document.getElementById('phase-selection').style.display = 'none';
    addLog("Menghidupkan Pokémon pengganti...", "#38bdf8");
    
    try {
        const response = await fetch('/api/battle/deploy-next', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerPokemonId: pokemonId })
        });
        const data = await response.json();
        
        if(data.success) {
            globalPokemonLeft = data.pokemonLeft;
            document.getElementById('player-name').textContent = data.player.name;
            setIconElement('sprite-player', 'icon-player', playerType);
            
            const sprPlayer = document.getElementById('sprite-player');
            sprPlayer.classList.remove('anim-faint');
            sprPlayer.style.opacity = '1';
            sprPlayer.style.transform = 'scale(1)';

            updateBattleUI({
                enemyCurrentHp: data.enemyCurrentHp, enemyMaxHp: data.enemyMaxHp,
                playerCurrentHp: data.player.hp, playerMaxHp: data.player.maxHp
            });

            renderSkillButtons(data.playerMoves);
            addLog(data.message, "#5eff81");
            addLog(`Kesempatan Pokémon Tersisa: [${globalPokemonLeft}/3]`, "#ffd966");
            
            document.getElementById('menu-main').style.display = 'grid';
            lockActionButtons(false);
        }
    } catch (err) { console.error(err); }
}

function renderSkillButtons(moves) {
    if (!moves) return;
    moves.forEach((move, i) => {
        const btn = document.getElementById('btn-skill-' + (i + 1));
        if (btn) btn.innerHTML = `<span style="font-size:14px;">${move.name}</span> <span style="font-size:10px; color:#94a3b8;">${move.type} | Pwr: ${move.power}</span>`;
    });
}

function setIconElement(spriteId, iconId, type) {
    const sprite = document.getElementById(spriteId);
    const icon = document.getElementById(iconId);
    if (!sprite || !icon) return;
    if(type === 'FIRE') { sprite.style.background = 'linear-gradient(135deg, #ef4444, #7f1d1d)'; icon.className = 'ti ti-flame'; }
    else if(type === 'WATER') { sprite.style.background = 'linear-gradient(135deg, #3b82f6, #1e3a8a)'; icon.className = 'ti ti-droplet'; }
    else if(type === 'GRASS') { sprite.style.background = 'linear-gradient(135deg, #10b981, #064e3b)'; icon.className = 'ti ti-leaf'; }
    else if(type === 'ELECTRIC') { sprite.style.background = 'linear-gradient(135deg, #f59e0b, #78350f)'; icon.className = 'ti ti-bolt'; }
    else if(type === 'ICE') { sprite.style.background = 'linear-gradient(135deg, #38bdf8, #0c4a6e)'; icon.className = 'ti ti-snowflake'; }
    else if(type === 'POISON') { sprite.style.background = 'linear-gradient(135deg, #a855f7, #4c1d95)'; icon.className = 'ti ti-skull'; }
}

function updateBattleUI(data) {
    if (data.enemyCurrentHp !== undefined && data.enemyMaxHp) {
        document.getElementById('enemy-hp-text').textContent = data.enemyCurrentHp + ' / ' + data.enemyMaxHp;
        const enemyPercent = Math.max(0, (data.enemyCurrentHp / data.enemyMaxHp * 100));
        const enemyBar = document.getElementById('enemy-hp-bar');
        if (enemyBar) {
            enemyBar.style.width = enemyPercent + '%';
            enemyBar.className = 'hp-bar-fill';
            if(enemyPercent <= 20) enemyBar.classList.add('danger');
            else if(enemyPercent <= 50) enemyBar.classList.add('warning');
        }
    }
    if (data.playerCurrentHp !== undefined && data.playerMaxHp) {
        document.getElementById('player-hp-text').textContent = data.playerCurrentHp + ' / ' + data.playerMaxHp;
        const playerPercent = Math.max(0, (data.playerCurrentHp / data.playerMaxHp * 100));
        const playerBar = document.getElementById('player-hp-bar');
        if (playerBar) {
            playerBar.style.width = playerPercent + '%';
            playerBar.className = 'hp-bar-fill';
            if(playerPercent <= 20) playerBar.classList.add('danger');
            else if(playerPercent <= 50) playerBar.classList.add('warning');
        }
    }
}

// =========================================
// SINKRONISASI ATTACK DENGAN FASE MENANGKAP REWARD
// =========================================
async function executeAttack(skillSlotIndex) {
    toggleMenu('menu-main');
    lockActionButtons(true);
    addLog(`Menyerang dengan skill slot ${skillSlotIndex}...`, "#ffd966");
    
    const sprPlayer = document.getElementById('sprite-player');
    const sprEnemy = document.getElementById('sprite-enemy');

    try {
        const response = await fetch('/api/battle/attack', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ skillSlot: skillSlotIndex })
        });
        const data = await response.json();

        // 1. Animasi menyerang
        if (sprPlayer) sprPlayer.classList.add('anim-attack-player');
        await sleep(300); 

        if (!data.hit) {
            addLog('Serangan meleset!', '#ff9999');
        } else {
            if (sprEnemy) sprEnemy.classList.add('anim-damage');
            addLog(`Serangan masuk! Damage: ${data.damage} (${data.effectiveness})`, '#5eff81');
            
            // Update darah musuh secara bertahap di UI
            if (data.enemyCurrentHp !== undefined && data.enemyMaxHp) {
                // Jika enemyAlive adalah false, langsung paksa cetak angka 0 di layar
                const displayHp = data.enemyAlive ? data.enemyCurrentHp : 0;
                
                document.getElementById('enemy-hp-text').textContent = displayHp + ' / ' + data.enemyMaxHp;
                
                const enemyPercent = Math.max(0, (displayHp / data.enemyMaxHp * 100));
                const enemyBar = document.getElementById('enemy-hp-bar');
                if (enemyBar) enemyBar.style.width = enemyPercent + '%';
            }
        }

        await sleep(400); 
        if (sprPlayer) sprPlayer.classList.remove('anim-attack-player');
        if (sprEnemy) sprEnemy.classList.remove('anim-damage');

        // === FASE FILTER REWARD: Jika HP musuh sudah habis ===
        if (!data.enemyAlive) {
            addLog('MUSUH TELAH DIKALAHKAN! Fase Menangkap Aktif! 🌟', '#fbbf24');
            addLog('Gunakan tombol "Catch" sekarang! Kamu punya 3x Kesempatan.', '#38bdf8');
            
            catchAttempts = 0; // Reset total lemparan bola
            
            const btnAttack = document.getElementById('btn-menu-attack');
            if (btnAttack) btnAttack.classList.add('disabled');
            
            toggleMenu('menu-main');
            lockActionButtons(false);
            return;
        }

        // 2. Giliran musuh membalas (Darah pemain berkurang setelah musuh menghantam)
        if (data.enemyAttackMessage) {
            await sleep(500);
            addLog(`Musuh mengamuk dan menyerang balik!`, "#fbbf24");
            if (sprEnemy) sprEnemy.classList.add('anim-attack-enemy');
            await sleep(300);
            
            if (sprPlayer) sprPlayer.classList.add('anim-damage');
            addLog(data.enemyAttackMessage + ' Damage: ' + (data.enemyAttackDamage || 0), '#ff9999');
            
            // Sinkronisasi penurunan darah player
            if (data.playerCurrentHp !== undefined && data.playerMaxHp) {
                document.getElementById('player-hp-text').textContent = data.playerCurrentHp + ' / ' + data.playerMaxHp;
                const playerPercent = Math.max(0, (data.playerCurrentHp / data.playerMaxHp * 100));
                const playerBar = document.getElementById('player-hp-bar');
                if (playerBar) {
                    playerBar.style.width = playerPercent + '%';
                    playerBar.className = 'hp-bar-fill';
                    if(playerPercent <= 20) playerBar.classList.add('danger');
                    else if(playerPercent <= 50) playerBar.classList.add('warning');
                }
            }
            
            await sleep(400);
            if (sprEnemy) sprEnemy.classList.remove('anim-attack-enemy');
            if (sprPlayer) sprPlayer.classList.remove('anim-damage');
        }
        
        // 3. Evaluasi kematian tim player
        if (data.playerAlive !== undefined && !data.playerAlive) {
            if (sprPlayer) sprPlayer.classList.add('anim-faint');
            addLog('Pokémon milikmu pingsan... 💀', '#ff5e5e');
            
            if (globalPokemonLeft > 1) {
                await sleep(1500);
                addLog("Pilih Pokémon pengganti selanjutnya dari timmu!", "#38bdf8");
                document.getElementById('phase-selection').style.display = 'block';
                loadDeployScreen(); 
            } else {
                // KONDISI KALAH TOTAL (0 NYAWA TIM SISA)
                await sleep(1000);
                addLog("Semua Pokémon andalanmu telah pingsan!", "#ef4444");
                addLog("Pokémon musuh melarikan diri... Kamu kalah! 💀", "#ef4444");
                
                // POPUP HADIAH HIBURAN DARI DATABASE
                if (data.rewardMessage) {
                    alert(`GAME OVER!\n\n${data.rewardMessage}\n\nSilakan cek tas inventory kamu.`);
                    addLog(data.rewardMessage, "#ffd966");
                }
                
                setTimeout(() => { window.location.href = '/home'; }, 2000);
            }
            return;
        }
    } catch (err) { console.error(err); }
    lockActionButtons(false);
}

// =========================================
// LOGIKA TOMBOL CATCH REWARD (3 KALI KESEMPATAN)
// =========================================
async function executeCatch() {
    const enemyHpText = document.getElementById('enemy-hp-text').textContent;
    if (!enemyHpText.startsWith('0 /')) {
        alert("Kamu tidak bisa menangkap sekarang! Kalahkan Pokémon musuh sampai HP-nya 0 terlebih dahulu!");
        return;
    }

    lockActionButtons(true);
    catchAttempts++; 
    
    addLog(`[Lemparan ke-${catchAttempts}/3] Melempar Pokéball...`, "#38bdf8");
    
    try {
        const response = await fetch('/api/battle/catch', { 
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ attempt: catchAttempts }) 
        });
        const data = await response.json();
        
        // Proteksi jika data yang dikembalikan null atau bermasalah
        if (!data) {
            addLog("Gagal mendapatkan respons dari server.", "#ff9999");
            lockActionButtons(false);
            return;
        }

        // Ambil pesan dari properti backend, gunakan fallback string jika undefined
        const logMessage = data.message || data.error || "Pokéball bergoyang... Namun gagal mengamankan Pokémon!";

        if (data.caught) {
            addLog(logMessage, '#5eff81');
            const sprEnemy = document.getElementById('sprite-enemy');
            if (sprEnemy) {
                sprEnemy.style.transition = "all 0.5s ease";
                sprEnemy.style.transform = 'scale(0.1) rotate(180deg)';
                sprEnemy.style.opacity = '0';
            }
            setTimeout(() => { window.location.href = '/collection'; }, 2500);
            return;
        } else {
            // Cetak pesan kegagalan asli dari Java (bukan undefined lagi)
            addLog(logMessage, '#ff9999');
            
            if (catchAttempts >= 3) {
                await sleep(1000);
                addLog("Kesempatan menangkap habis! Pokémon liar melarikan diri ke hutan... 🏃‍♂️", "#ef4444");
                const sprEnemy = document.getElementById('sprite-enemy');
                if (sprEnemy) {
                    sprEnemy.style.transition = "all 0.8s ease";
                    sprEnemy.style.transform = "translateX(300px)";
                    sprEnemy.style.opacity = "0";
                }
                setTimeout(() => { window.location.href = '/home'; }, 3000);
                return;
            } else {
                addLog(`Ayo coba lagi! Sisa lemparan: ${3 - catchAttempts}`, '#ffd966');
            }
        }
    } catch (err) { 
        console.error("Error pada mekanisme catch:", err);
        addLog("Terjadi kesalahan jaringan saat melempar bola.", "#ff9999");
    }
    lockActionButtons(false);
}

async function checkPostActionSurvival(data) {
    if (data.playerAlive !== undefined && !data.playerAlive) {
        const sprPlayer = document.getElementById('sprite-player');
        const sprEnemy = document.getElementById('sprite-enemy');
        if (sprPlayer) sprPlayer.classList.add('anim-faint');
        addLog('Pokémon milikmu pingsan... 💀', '#ff5e5e');
        
        if (globalPokemonLeft > 1) {
            await sleep(1500);
            addLog("Pilih Pokémon pengganti selanjutnya dari timmu!", "#38bdf8");
            document.getElementById('phase-selection').style.display = 'block';
            loadDeployScreen();
        } else {
            await sleep(1000);
            addLog("Semua Pokémon andalanmu telah pingsan!", "#ef4444");
            if (sprEnemy) {
                sprEnemy.style.transition = "all 0.8s ease";
                sprEnemy.style.transform = "translateX(300px)";
                sprEnemy.style.opacity = "0";
            }
            addLog("Pokémon musuh berhasil kabur melarikan diri... Kamu kalah! 💀", "#ef4444");
            setTimeout(() => { window.location.href = '/home'; }, 3000);
        }
        return true;
    }
    return false;
}

async function executeItem(itemName) {
    toggleMenu('menu-main');
    lockActionButtons(true);
    addLog(`Menggunakan ${itemName}...`, "#ffd966");
    
    try {
        const response = await fetch('/api/battle/item', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ itemName: itemName })
        });
        const data = await response.json();
        
        if (data.error) {
            addLog(data.error, '#ff9999');
            lockActionButtons(false);
            return;
        }
        addLog(data.message, '#5eff81');
        if (data.playerCurrentHp !== undefined) updateBattleUI(data);

        if (data.enemyAttackMessage) {
            await sleep(500);
            const sprEnemy = document.getElementById('sprite-enemy');
            const sprPlayer = document.getElementById('sprite-player');
            
            if (sprEnemy) sprEnemy.classList.add('anim-attack-enemy');
            await sleep(300);
            if (sprPlayer) sprPlayer.classList.add('anim-damage');
            addLog(data.enemyAttackMessage + ' Damage: ' + (data.enemyAttackDamage || 0), '#ff9999');
            updateBattleUI(data);
            
            await sleep(400);
            if (sprEnemy) sprEnemy.classList.remove('anim-attack-enemy');
            if (sprPlayer) sprPlayer.classList.remove('anim-damage');

            if (await checkPostActionSurvival(data)) return;
        }
    } catch (err) { console.error(err); }
    lockActionButtons(false);
}

async function loadItemList() {
    try {
        const response = await fetch('/api/user/inventory');
        const items = await response.json();
        const container = document.getElementById('item-list-container');
        if (!container) return;
        
        if (!items || items.length === 0) {
            container.innerHTML = '<p style="color: var(--text3); text-align: center;">Kamu tidak punya item</p>';
            return;
        }
        container.innerHTML = items.map(item =>
        `<button class="btn-action" onclick="executeItem('${item.name}')" style="flex-direction:row; justify-content:space-between; padding:5px 15px;">
            <span>${item.name}</span> <span style="background:var(--accent); padding:2px 8px; border-radius:10px; font-size:10px;">x${item.quantity}</span>
        </button>`).join('');
    } catch (e) {
        console.error("Gagal memuat item", e);
    }
}

// =========================================================================
// 7. AUTO-INIT INITIALIZATION BERDASARKAN SELEKTOR HALAMAN HTML
// =========================================================================
document.addEventListener("DOMContentLoaded", () => {
    const pageType = document.body.dataset.page;
    if (pageType === 'home') {
        loadDashboardStats();
    } else if (pageType === 'collection') {
        fetchCollectionData();
    } else if (pageType === 'inventory') {
        fetchInventoryData();
    } else if (pageType === 'battle') {
        loadDeployScreen();
    }
});

// =========================================================================
// 8. LOGIKA TOMBOL SWAP MANUAL RANDOM (MAKSIMAL 3 KALI)
// =========================================================================
async function executeSwapRandom() {
    // Pengaman: Jangan biarkan swap jika musuh sudah mati (fase menangkap)
    const enemyHpText = document.getElementById('enemy-hp-text').textContent;
    if (enemyHpText.startsWith('0 /')) return;

    if (globalSwapLeft <= 0) {
        addLog("Jatah ganti Pokémon acak kamu sudah habis pertempuran ini!", "#ff9999");
        return;
    }

    if (!confirm("Apakah kamu yakin ingin menukar Pokémon saat ini secara acak? (Musuh akan langsung menyerangmu saat pergantian!)")) {
        return;
    }

    toggleMenu('menu-main');
    lockActionButtons(true);
    addLog("Menarik kembali Pokémon aktif dan mengocok dadu cadangan...", "#ffd966");

    try {
        const response = await fetch('/api/battle/swap-random', { method: 'POST' });
        const data = await response.json();

        if (data.error) {
            addLog(data.error, '#ff9999');
            lockActionButtons(false);
            return;
        }

        globalSwapLeft = data.swapLeft;
        
        // Perbarui teks kuota di tombol UI
        const btnSwap = document.getElementById('btn-menu-swap');
        if (btnSwap) btnSwap.innerHTML = `<i class="ti ti-refresh"></i> Swap (${globalSwapLeft})`;

        // 1. Efek visual pergantian pemain
        const sprPlayer = document.getElementById('sprite-player');
        if (sprPlayer) {
            sprPlayer.style.transition = "all 0.3s ease";
            sprPlayer.style.transform = "scale(0)"; // Mengecil hilang
            await sleep(300);
            
            // Set data Pokémon baru yang keluar dari kocokan
            document.getElementById('player-name').textContent = data.player.name;
            
            // Atur ulang warna lingkaran elemen di main.js
            let pType = data.player.type;
            if(pType === 'FIRE') { sprPlayer.style.background = 'linear-gradient(135deg, #ef4444, #7f1d1d)'; document.getElementById('icon-player').className = 'ti ti-flame'; }
            else if(pType === 'WATER') { sprPlayer.style.background = 'linear-gradient(135deg, #3b82f6, #1e3a8a)'; document.getElementById('icon-player').className = 'ti ti-droplet'; }
            else if(pType === 'GRASS') { sprPlayer.style.background = 'linear-gradient(135deg, #10b981, #064e3b)'; document.getElementById('icon-player').className = 'ti ti-leaf'; }
            else if(pType === 'ELECTRIC') { sprPlayer.style.background = 'linear-gradient(135deg, #f59e0b, #78350f)'; document.getElementById('icon-player').className = 'ti ti-bolt'; }
            else if(pType === 'ICE') { sprPlayer.style.background = 'linear-gradient(135deg, #38bdf8, #0c4a6e)'; document.getElementById('icon-player').className = 'ti ti-snowflake'; }
            else if(pType === 'POISON') { sprPlayer.style.background = 'linear-gradient(135deg, #a855f7, #4c1d95)'; document.getElementById('icon-player').className = 'ti ti-skull'; }

            sprPlayer.style.transform = "scale(1)"; // Muncul membesar kembali
        }

        updateBattleUI({
            enemyCurrentHp: data.enemyCurrentHp, enemyMaxHp: data.enemyMaxHp,
            playerCurrentHp: data.playerCurrentHp, playerMaxHp: data.playerMaxHp
        });

        renderSkillButtons(data.playerMoves);
        addLog(data.message, "#5eff81");

        // 2. Animasi serangan musuh yang memanfaatkan kelengahan swap pemain
        if (data.enemyAttackMessage) {
            await sleep(600);
            addLog(`Musuh mengambil kesempatan saat kamu mengganti Pokémon!`, "#fbbf24");
            const sprEnemy = document.getElementById('sprite-enemy');
            if (sprEnemy) sprEnemy.classList.add('anim-attack-enemy');
            await sleep(300);

            if (sprPlayer) sprPlayer.classList.add('anim-damage');
            addLog(data.enemyAttackMessage + ' Damage: ' + (data.enemyAttackDamage || 0), '#ff9999');
            
            updateBattleUI(data);

            await sleep(400);
            if (sprEnemy) sprEnemy.classList.remove('anim-attack-enemy');
            if (sprPlayer) sprPlayer.classList.remove('anim-damage');

            // Cek jika Pokémon baru hasil kocokan langsung pingsan akibat hantaman brutal musuh
            if (await checkPostActionSurvival(data)) return;
        }

    } catch (err) {
        console.error(err);
    }
    lockActionButtons(false);
}