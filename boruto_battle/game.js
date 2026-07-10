const characters = [
  { name: 'Naruto', imageSrc: 'assets/naruto.png', emoji: '🦊', color: '#ff9900', skill: { name: 'Rasengan', damage: 30 } },
  { name: 'Sasuke', imageSrc: 'assets/sasuke.png', emoji: '⚡', color: '#3333cc', skill: { name: 'Chidori', damage: 30 } },
  { name: 'Sakura', imageSrc: 'assets/sakura.png', emoji: '🌸', color: '#ff99cc', skill: { name: 'Cherry Blossom Clash', damage: 25 } },
  { name: 'Kakashi', imageSrc: 'assets/kakashi.png', emoji: '📖', color: '#999999', skill: { name: 'Raikiri', damage: 35 } },
  { name: 'Boruto', imageSrc: 'assets/boruto.png', emoji: '🌀', color: '#ffcc00', skill: { name: 'Vanishing Rasengan', damage: 28 } },
  { name: 'Sarada', imageSrc: 'assets/sarada.png', emoji: '👓', color: '#cc0000', skill: { name: 'Sharingan Strike', damage: 25 } },
  { name: 'Mitsuki', imageSrc: 'assets/mitsuki.png', emoji: '🐍', color: '#66ccff', skill: { name: 'Snake Lightning', damage: 25 } },
  { name: 'Konohamaru', imageSrc: 'assets/konohamaru.png', emoji: '🧣', color: '#339933', skill: { name: 'Burning Ash', damage: 20 } },
  { name: 'Shikamaru', imageSrc: 'assets/shikamaru.png', emoji: '☁️', color: '#666666', skill: { name: 'Shadow Possession', damage: 20 } },
  { name: 'Hinata', imageSrc: 'assets/hinata.png', emoji: '👁️', color: '#cc99ff', skill: { name: 'Gentle Step', damage: 20 } },
  { name: 'Gaara', imageSrc: 'assets/gaara.png', emoji: '⏳', color: '#993333', skill: { name: 'Sand Coffin', damage: 28 } },
  { name: 'Itachi', imageSrc: 'assets/itachi.png', emoji: '🦅', color: '#000000', skill: { name: 'Amaterasu', damage: 40 } },
  { name: 'Jiraiya', imageSrc: 'assets/jiraiya.png', emoji: '🐸', color: '#ff3300', skill: { name: 'Toad Oil Fireball', damage: 35 } },
  { name: 'Tsunade', imageSrc: 'assets/tsunade.png', emoji: '🐌', color: '#009933', skill: { name: '100 Healings Strike', damage: 35 } },
  { name: 'Orochimaru', imageSrc: 'assets/orochimaru.png', emoji: '🐍', color: '#660066', skill: { name: 'Striking Shadow Snake', damage: 30 } },
  { name: 'Minato', imageSrc: 'assets/minato.png', emoji: '⚡', color: '#ffff33', skill: { name: 'Flying Raijin', damage: 35 } },
  { name: 'Kawaki', imageSrc: 'assets/kawaki.png', emoji: '♦️', color: '#000033', skill: { name: 'Karma Blast', damage: 35 } },
  { name: 'Jigen', imageSrc: 'assets/jigen.png', emoji: '⬛', color: '#111111', skill: { name: 'Black Rods', damage: 40 } },
  { name: 'Momoshiki', imageSrc: 'assets/momoshiki.png', emoji: '👁️‍🗨️', color: '#ffffff', skill: { name: 'Takami Musubinokami', damage: 45 } },
  { name: 'Isshiki', imageSrc: 'assets/isshiki.png', emoji: '👁️‍🗨️', color: '#eeeeee', skill: { name: 'Sukunahikona', damage: 50 } }
];

let loadedImages = 0;

function preloadImages(onComplete) {
  for (let i = 0; i < characters.length; i++) {
    const img = new Image();
    img.onload = function() {
      loadedImages++;
      if (loadedImages === characters.length) {
        onComplete();
      }
    };
    img.onerror = function() {
      console.log("Failed to load image: " + characters[i].imageSrc);
      loadedImages++; // Count it anyway so game proceeds
      if (loadedImages === characters.length) {
        onComplete();
      }
    };
    img.src = characters[i].imageSrc;
    characters[i].imgObj = img;
  }
}

function initGame() {
  let fbInitialized = false;

  const startSequence = () => {
    preloadImages(() => {
      startGame();
    });
  };

  const startLocalGame = () => {
    if (!fbInitialized) {
      fbInitialized = true;
      startSequence();
    }
  };

  setTimeout(startLocalGame, 1000);

  if (typeof FBInstant !== 'undefined' && FBInstant.initializeAsync) {
    FBInstant.initializeAsync().then(function() {
      var loaded = 0;
      var interval = setInterval(function() {
        loaded += 10;
        if (FBInstant.setLoadingProgress) FBInstant.setLoadingProgress(loaded);
        if (loaded >= 100) {
          clearInterval(interval);
          if (FBInstant.startGameAsync) {
             FBInstant.startGameAsync().then(function() {
               if (!fbInitialized) {
                 fbInitialized = true;
                 startSequence();
               }
             }).catch(startLocalGame);
          } else startLocalGame();
        }
      }, 50);
    }).catch(startLocalGame);
  } else startLocalGame();
}

window.addEventListener('load', initGame);

function startGame() {
  const canvas = document.getElementById('gameCanvas');
  const ctx = canvas.getContext('2d');

  let charWidth = 60;
  let charHeight = 60;
  let gridX = 0;
  const cols = 4;
  const padding = 10;

  function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    charWidth = (canvas.width - (cols + 1) * padding) / cols;
    if (charWidth > 80) charWidth = 80;
    charHeight = charWidth;
    gridX = (canvas.width - (cols * charWidth + (cols - 1) * padding)) / 2;
  }
  window.addEventListener('resize', resize);
  resize();

  let gameState = 'select'; // select, playing, win, lose
  let playerChar = null;
  let enemyChar = null;
  let playerHealth = 100;
  let enemyHealth = 100;

  let activeSkillText = '';
  let activeSkillTimer = 0;

  let playerShake = 0;
  let enemyShake = 0;
  let floatingTexts = [];

  const playerSprite = { x: canvas.width / 4, y: canvas.height - 250, width: 100, height: 100 };
  const enemySprite = { x: (canvas.width / 4) * 3, y: canvas.height - 250, width: 100, height: 100 };

  const inviteBtn = { x: canvas.width / 2 - 100, y: canvas.height - 150, w: 200, h: 50 };

  function addFloatingText(x, y, text, color) {
    floatingTexts.push({ x, y, text, color, life: 40 });
  }

  function drawHealthBar(x, y, health, maxHealth, name) {
    ctx.fillStyle = 'white';
    ctx.font = '20px Arial';
    ctx.fillText(name, x, y - 20);
    ctx.fillStyle = 'red';
    ctx.fillRect(x - 50, y, 100, 15);
    ctx.fillStyle = 'green';
    ctx.fillRect(x - 50, y, Math.max(0, health), 15);
  }

  function drawCharacter(x, y, w, h, charData, isEnemy) {
    // 1. Draw background block
    ctx.fillStyle = charData.color;
    ctx.fillRect(x - w/2, y - h/2, w, h);

    // 2. Draw Emoji as a baseline visual so it's never completely blank
    ctx.fillStyle = 'white';
    ctx.font = (w/2) + 'px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(charData.emoji, x, y);

    // 3. Draw actual image if loaded (this will render over the emoji if it's a real, non-transparent image)
    if (charData.imgObj && charData.imgObj.complete && charData.imgObj.naturalHeight !== 0) {
      ctx.drawImage(charData.imgObj, x - w/2, y - h/2, w, h);
    }

    // 4. Draw border
    ctx.strokeStyle = isEnemy ? 'red' : 'white';
    ctx.lineWidth = 3;
    ctx.strokeRect(x - w/2, y - h/2, w, h);
  }

  function shareGame() {
    if (typeof FBInstant !== 'undefined' && FBInstant.shareAsync) {
      FBInstant.shareAsync({
        intent: 'INVITE',
        image: '',
        text: 'Come battle with me in Boruto Battle!',
        data: { myReplayData: '...' },
      }).then(() => console.log("Shared successfully!")).catch(e => console.log("Share failed", e));
    } else {
      alert("Invite Friends triggered! (Works in Facebook app)");
    }
  }

  function gameLoop() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.fillStyle = 'white';
    ctx.font = 'bold 36px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'alphabetic';
    ctx.fillText('Boruto Battle', canvas.width / 2, 60);

    if (gameState === 'select') {
      ctx.font = '24px Arial';
      ctx.fillStyle = '#ffcc00';
      ctx.fillText('Select Your Hero', canvas.width / 2, 100);

      for(let i=0; i<characters.length; i++) {
         let col = i % cols;
         let row = Math.floor(i / cols);
         let cx = gridX + col * (charWidth + padding) + charWidth/2;
         let cy = 130 + row * (charHeight + padding) + charHeight/2;
         drawCharacter(cx, cy, charWidth, charHeight, characters[i], false);
      }
    } else if (gameState === 'playing') {
      if (playerShake > 0) playerShake -= 1;
      if (enemyShake > 0) enemyShake -= 1;

      let px = playerSprite.x + (Math.random() - 0.5) * playerShake;
      let py = playerSprite.y + (Math.random() - 0.5) * playerShake;
      let ex = enemySprite.x + (Math.random() - 0.5) * enemyShake;
      let ey = enemySprite.y + (Math.random() - 0.5) * enemyShake;

      drawCharacter(px, py, playerSprite.width, playerSprite.height, playerChar, false);
      drawCharacter(ex, ey, enemySprite.width, enemySprite.height, enemyChar, true);

      drawHealthBar(playerSprite.x, playerSprite.y - 80, playerHealth, 100, playerChar.name);
      drawHealthBar(enemySprite.x, enemySprite.y - 80, enemyHealth, 100, enemyChar.name);

      ctx.fillStyle = '#ffcc00';
      ctx.font = '24px Arial';
      ctx.fillText('Tap Screen to Attack!', canvas.width / 2, canvas.height - 50);

      ctx.fillStyle = '#4267B2';
      ctx.fillRect(inviteBtn.x, inviteBtn.y, inviteBtn.w, inviteBtn.h);
      ctx.fillStyle = 'white';
      ctx.font = 'bold 20px Arial';
      ctx.textBaseline = 'middle';
      ctx.fillText('Invite Friends', canvas.width / 2, inviteBtn.y + inviteBtn.h / 2);

      if (activeSkillTimer > 0) {
        ctx.fillStyle = '#00ffff';
        ctx.font = 'bold 24px Arial';
        ctx.fillText(activeSkillText, canvas.width / 2, canvas.height / 2);
        activeSkillTimer--;
      }

      for(let i = floatingTexts.length - 1; i >= 0; i--) {
         let ft = floatingTexts[i];
         ctx.fillStyle = ft.color;
         ctx.font = 'bold 24px Arial';
         ctx.globalAlpha = ft.life / 40;
         ctx.fillText(ft.text, ft.x, ft.y);
         ctx.globalAlpha = 1.0;
         ft.y -= 2;
         ft.life--;
         if(ft.life <= 0) floatingTexts.splice(i, 1);
      }

      if (Math.random() < 0.02) {
        let dmg = 5;
        let isSkill = false;
        if (Math.random() < 0.2) {
           dmg = enemyChar.skill.damage;
           activeSkillText = enemyChar.name + " uses " + enemyChar.skill.name + "!";
           activeSkillTimer = 60;
           isSkill = true;
        }
        playerHealth -= dmg;
        playerShake = isSkill ? 20 : 10;
        addFloatingText(playerSprite.x, playerSprite.y - 50, "-" + dmg, "red");
        if (playerHealth <= 0) gameState = 'lose';
      }

    } else if (gameState === 'win' || gameState === 'lose') {
      ctx.fillStyle = gameState === 'win' ? '#00ff00' : '#ff0000';
      ctx.font = 'bold 50px Arial';
      ctx.fillText(gameState === 'win' ? 'YOU WIN!' : 'YOU LOSE', canvas.width / 2, canvas.height / 2);
      ctx.fillStyle = 'white';
      ctx.font = '24px Arial';
      ctx.fillText('Tap to restart', canvas.width / 2, canvas.height / 2 + 60);
    }

    requestAnimationFrame(gameLoop);
  }

  gameLoop();

  function handleInput(x, y) {
    if (gameState === 'select') {
      for(let i=0; i<characters.length; i++) {
         let col = i % cols;
         let row = Math.floor(i / cols);
         let cx = gridX + col * (charWidth + padding);
         let cy = 130 + row * (charHeight + padding);
         if (x >= cx && x <= cx + charWidth && y >= cy && y <= cy + charHeight) {
            playerChar = characters[i];
            enemyChar = characters[Math.floor(Math.random() * characters.length)];
            playerHealth = 100;
            enemyHealth = 100;
            playerShake = 0;
            enemyShake = 0;
            floatingTexts = [];
            activeSkillTimer = 0;
            gameState = 'playing';
            return;
         }
      }
    } else if (gameState === 'playing') {
      if (x >= inviteBtn.x && x <= inviteBtn.x + inviteBtn.w &&
          y >= inviteBtn.y && y <= inviteBtn.y + inviteBtn.h) {
          shareGame();
          return;
      }

      let dmg = 15;
      let isSkill = false;
      if (Math.random() < 0.3) {
         dmg = playerChar.skill.damage;
         activeSkillText = playerChar.name + " uses " + playerChar.skill.name + "!";
         activeSkillTimer = 60;
         isSkill = true;
      }
      enemyHealth -= dmg;
      enemyShake = isSkill ? 20 : 10;
      addFloatingText(enemySprite.x, enemySprite.y - 50, "-" + dmg, "yellow");

      if (enemyHealth <= 0) {
        gameState = 'win';
      }
    } else {
      gameState = 'select';
    }
  }

  canvas.addEventListener('touchstart', function(e) {
    e.preventDefault();
    handleInput(e.touches[0].clientX, e.touches[0].clientY);
  }, { passive: false });

  canvas.addEventListener('mousedown', function(e) {
    handleInput(e.clientX, e.clientY);
  });
}
