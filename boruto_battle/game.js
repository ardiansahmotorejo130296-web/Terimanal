// Initialize Facebook Instant Games SDK
FBInstant.initializeAsync()
  .then(function() {
    // Simulate loading assets
    var loaded = 0;
    var interval = setInterval(function() {
      loaded += 10;
      FBInstant.setLoadingProgress(loaded);

      if (loaded >= 100) {
        clearInterval(interval);
        // Start the game when loading is complete
        FBInstant.startGameAsync().then(function() {
          startGame();
        });
      }
    }, 100);
  });

function startGame() {
  const canvas = document.getElementById('gameCanvas');
  const ctx = canvas.getContext('2d');

  // Resize canvas to fill the screen
  function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }
  window.addEventListener('resize', resize);
  resize();

  // Simple player object
  const player = {
    x: canvas.width / 2,
    y: canvas.height - 100,
    width: 50,
    height: 50,
    color: 'orange',
    speed: 5
  };

  // Game loop
  function gameLoop() {
    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw background
    ctx.fillStyle = '#222';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw title
    ctx.fillStyle = 'white';
    ctx.font = '30px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('Boruto Battle', canvas.width / 2, 50);

    // Draw player
    ctx.fillStyle = player.color;
    ctx.fillRect(player.x - player.width/2, player.y - player.height/2, player.width, player.height);

    // Request next frame
    requestAnimationFrame(gameLoop);
  }

  // Start the game loop
  gameLoop();

  // Basic touch controls
  canvas.addEventListener('touchstart', function(e) {
    const touchX = e.touches[0].clientX;
    if (touchX < canvas.width / 2) {
      player.x -= 20;
    } else {
      player.x += 20;
    }

    // Keep player on screen
    if (player.x < player.width/2) player.x = player.width/2;
    if (player.x > canvas.width - player.width/2) player.x = canvas.width - player.width/2;
  });
}
