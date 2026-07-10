const characters = [
  { name: 'Naruto', emoji: '🦊', color: '#ff9900' },
  { name: 'Sasuke', emoji: '⚡', color: '#3333cc' },
  { name: 'Sakura', emoji: '🌸', color: '#ff99cc' },
  { name: 'Kakashi', emoji: '📖', color: '#999999' },
  { name: 'Boruto', emoji: '🌀', color: '#ffcc00' },
  { name: 'Sarada', emoji: '👓', color: '#cc0000' },
  { name: 'Mitsuki', emoji: '🐍', color: '#66ccff' },
  { name: 'Konohamaru', emoji: '🧣', color: '#339933' },
  { name: 'Shikamaru', emoji: '☁️', color: '#666666' },
  { name: 'Hinata', emoji: '👁️', color: '#cc99ff' },
  { name: 'Gaara', emoji: '⏳', color: '#993333' },
  { name: 'Itachi', emoji: '🦅', color: '#000000' },
  { name: 'Jiraiya', emoji: '🐸', color: '#ff3300' },
  { name: 'Tsunade', emoji: '🐌', color: '#009933' },
  { name: 'Orochimaru', emoji: '🐍', color: '#660066' },
  { name: 'Minato', emoji: '⚡', color: '#ffff33' },
  { name: 'Kawaki', emoji: '♦️', color: '#000033' },
  { name: 'Jigen', emoji: '⬛', color: '#111111' },
  { name: 'Momoshiki', emoji: '👁️‍🗨️', color: '#ffffff' },
  { name: 'Isshiki', emoji: '👁️‍🗨️', color: '#eeeeee' }
];

function initGame() {
  let gameStarted = false;

  const startLocalGame = () => {
    if (!gameStarted) {
      console.log("Starting game locally/fallback");
      gameStarted = true;
      startGame();
    }
  };

  // Fallback timeout to ensure the game starts even if SDK hangs
  setTimeout(startLocalGame, 1000);

  if (typeof FBInstant !== 'undefined' && FBInstant.initializeAsync) {
    FBInstant.initializeAsync().then(function() {
      var loaded = 0;
      var interval = setInterval(function() {
        loaded += 10;
        if (FBInstant.setLoadingProgress) {
          FBInstant.setLoadingProgress(loaded);
        }

        if (loaded >= 100) {
          clearInterval(interval);
          if (FBInstant.startGameAsync) {
             FBInstant.startGameAsync().then(function() {
               if (!gameStarted) {
                 gameStarted = true;
                 startGame();
               }
             }).catch(startLocalGame);
          } else {
             startLocalGame();
          }
        }
      }, 50);
    }).catch(startLocalGame);
  } else {
    startLocalGame();
  }
}

// Wait for DOM
window.addEventListener('load', initGame);

function startGame() {
  const canvas = document.getElementById('gameCanvas');
  const ctx = canvas.getContext('2d');

  function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }
  window.addEventListener('resize', resize);
  resize();

  let playerChar = characters[Math.floor(Math.random() * characters.length)];
  let enemyChar = characters[Math.floor(Math.random() * characters.length)];

  let playerHealth = 100;
  let enemyHealth = 100;

  let gameState = 'playing'; // 'playing', 'win', 'lose'

  const playerSprite = {
    x: canvas.width / 4,
    y: canvas.height - 250,
    width: 100,
    height: 100
  };

  const enemySprite = {
    x: (canvas.width / 4) * 3,
    y: canvas.height - 250,
    width: 100,
    height: 100
  };

  function drawHealthBar(x, y, health, maxHealth, name) {
    ctx.fillStyle = 'white';
    ctx.font = '20px Arial';
    ctx.fillText(name, x, y - 20);

    ctx.fillStyle = 'red';
    ctx.fillRect(x - 50, y, 100, 15);

    ctx.fillStyle = 'green';
    ctx.fillRect(x - 50, y, Math.max(0, health), 15);
  }

  function drawCharacter(sprite, charData, isEnemy) {
    // Draw background block
    ctx.fillStyle = charData.color;
    ctx.fillRect(sprite.x - sprite.width/2, sprite.y - sprite.height/2, sprite.width, sprite.height);

    // Draw border
    ctx.strokeStyle = isEnemy ? 'red' : 'white';
    ctx.lineWidth = 3;
    ctx.strokeRect(sprite.x - sprite.width/2, sprite.y - sprite.height/2, sprite.width, sprite.height);

    // Draw Emoji
    ctx.fillStyle = 'white';
    ctx.font = '50px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(charData.emoji, sprite.x, sprite.y);
  }

  function gameLoop() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Background
    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Title
    ctx.fillStyle = 'white';
    ctx.font = 'bold 40px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'alphabetic';
    ctx.fillText('Boruto Battle', canvas.width / 2, 60);

    if (gameState === 'playing') {
      drawCharacter(playerSprite, playerChar, false);
      drawCharacter(enemySprite, enemyChar, true);

      drawHealthBar(playerSprite.x, playerSprite.y - 80, playerHealth, 100, playerChar.name);
      drawHealthBar(enemySprite.x, enemySprite.y - 80, enemyHealth, 100, enemyChar.name);

      ctx.fillStyle = '#ffcc00';
      ctx.font = '24px Arial';
      ctx.fillText('Tap Screen to Attack!', canvas.width / 2, canvas.height - 80);

      // AI attack logic
      if (Math.random() < 0.03) {
        playerHealth -= 5;
        if (playerHealth <= 0) gameState = 'lose';
      }

    } else if (gameState === 'win') {
      ctx.fillStyle = '#00ff00';
      ctx.font = 'bold 50px Arial';
      ctx.fillText('YOU WIN!', canvas.width / 2, canvas.height / 2);
      ctx.fillStyle = 'white';
      ctx.font = '24px Arial';
      ctx.fillText('Tap to restart', canvas.width / 2, canvas.height / 2 + 60);
    } else if (gameState === 'lose') {
      ctx.fillStyle = '#ff0000';
      ctx.font = 'bold 50px Arial';
      ctx.fillText('YOU LOSE', canvas.width / 2, canvas.height / 2);
      ctx.fillStyle = 'white';
      ctx.font = '24px Arial';
      ctx.fillText('Tap to restart', canvas.width / 2, canvas.height / 2 + 60);
    }

    requestAnimationFrame(gameLoop);
  }

  gameLoop();

  function handleInput(x, y) {
    if (gameState === 'playing') {
      // Player attacks
      enemyHealth -= 15;
      if (enemyHealth <= 0) {
        gameState = 'win';
      }
    } else {
      // Restart
      playerChar = characters[Math.floor(Math.random() * characters.length)];
      enemyChar = characters[Math.floor(Math.random() * characters.length)];
      playerHealth = 100;
      enemyHealth = 100;
      gameState = 'playing';
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
