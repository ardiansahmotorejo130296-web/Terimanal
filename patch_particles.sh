sed -i '/let floatingTexts = \[\];/a\  let particles = \[\];' boruto_battle/game.js
sed -i '/function addFloatingText(x, y, text, color) {/i\  function createParticles(x, y, color) {\n    for (let i = 0; i < 15; i++) {\n      particles.push({\n        x: x,\n        y: y,\n        vx: (Math.random() - 0.5) * 10,\n        vy: (Math.random() - 0.5) * 10,\n        life: 20 + Math.random() * 20,\n        color: color,\n        size: 2 + Math.random() * 4\n      });\n    }\n  }' boruto_battle/game.js
sed -i '/floatingTexts = \[\];/a\            particles = \[\];' boruto_battle/game.js

sed -i '/for(let i = floatingTexts.length - 1; i >= 0; i--) {/i\      for (let i = particles.length - 1; i >= 0; i--) {\n        let p = particles[i];\n        ctx.fillStyle = p.color;\n        ctx.beginPath();\n        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);\n        ctx.fill();\n        p.x += p.vx;\n        p.y += p.vy;\n        p.life--;\n        if (p.life <= 0) particles.splice(i, 1);\n      }' boruto_battle/game.js

sed -i '/addFloatingText(playerSprite.x, playerSprite.y - 50, "-" + dmg, "red");/a\        createParticles(playerSprite.x, playerSprite.y, "red");' boruto_battle/game.js
sed -i '/addFloatingText(enemySprite.x, enemySprite.y - 50, "-" + dmg, "yellow");/a\      createParticles(enemySprite.x, enemySprite.y, "yellow");' boruto_battle/game.js
