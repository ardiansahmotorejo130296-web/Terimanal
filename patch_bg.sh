sed -i 's/const canvas = document.getElementById('\''gameCanvas'\'');/const canvas = document.getElementById('\''gameCanvas'\'');\n  const bgImg = new Image();\n  bgImg.src = "assets\/background.png";/' boruto_battle/game.js
sed -i 's/ctx.fillStyle = '\'#1a1a2e\'';/if (bgImg.complete \&\& bgImg.naturalHeight !== 0) {\n      ctx.drawImage(bgImg, 0, 0, canvas.width, canvas.height);\n    } else {\n      ctx.fillStyle = '\'#1a1a2e\'';/' boruto_battle/game.js
sed -i 's/ctx.fillRect(0, 0, canvas.width, canvas.height);/ctx.fillRect(0, 0, canvas.width, canvas.height);\n    }/' boruto_battle/game.js
