package com.example.pocketarcade.generator

import kotlin.random.Random

data class GeneratedGameSpec(
    val title: String,
    val prompt: String,
    val htmlCode: String,
    val isOffline: Boolean
)

object OfflineGameGenerator {

    private data class ColorPalette(
        val name: String,
        val bg: String,
        val primary: String,
        val secondary: String,
        val accent: String
    )

    private val palettes = listOf(
        ColorPalette("Cyber Mint", "#05050A", "#00F5D4", "#FF007F", "#FEE440"),
        ColorPalette("Synthwave", "#090414", "#F72585", "#4CC9F0", "#B5179E"),
        ColorPalette("Matrix Core", "#030A05", "#00FF66", "#00B8FF", "#E0FF00"),
        ColorPalette("Solar Flare", "#0D0505", "#FF5400", "#FFBD00", "#00BBF9")
    )

    private val randomPromptIdeas = listOf(
        "Neon Space Dodger",
        "Cyber Brick Breaker",
        "Pulse Reflex Tapper",
        "Laser Core Defender",
        "Hyper Tunnel Runner",
        "Plasma Orb Catcher"
    )

    fun pickRandomPrompt(): String = randomPromptIdeas.random()

    fun generateRandomGame(customPrompt: String? = null): GeneratedGameSpec {
        val palette = palettes.random()
        val normalized = customPrompt?.lowercase().orEmpty()

        val mode = when {
            normalized.contains("brick") || normalized.contains("break") || normalized.contains("pong") -> 1
            normalized.contains("tap") || normalized.contains("reflex") || normalized.contains("orb") -> 2
            normalized.contains("dodge") || normalized.contains("space") || normalized.contains("runner") -> 0
            else -> Random.nextInt(3)
        }

        return when (mode) {
            0 -> buildNeonDodger(palette, customPrompt)
            1 -> buildBrickBreaker(palette, customPrompt)
            else -> buildReflexTapper(palette, customPrompt)
        }
    }

    private fun buildNeonDodger(palette: ColorPalette, customPrompt: String?): GeneratedGameSpec {
        val speedScale = listOf("1.0", "1.2", "1.35").random()
        val enemyShape = listOf("block", "diamond").random()
        val title = customPrompt?.takeIf { it.isNotBlank() } ?: "${palette.name} Dodger"
        val html = """
<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; user-select: none; -webkit-user-select: none; }
  body { background: ${palette.bg}; overflow: hidden; touch-action: none; font-family: monospace; }
  canvas { display: block; width: 100vw; height: 100vh; }
</style>
</head>
<body>
<canvas id="c"></canvas>
<script>
const canvas = document.getElementById('c');
const ctx = canvas.getContext('2d');
let W, H;
function resize() { W = canvas.width = window.innerWidth; H = canvas.height = window.innerHeight; }
window.addEventListener('resize', resize);
resize();

let score = 0;
let gameOver = false;
let player = { x: W / 2, y: H * 0.8, r: 16 };
let obstacles = [];
let particles = [];
let spawnTimer = 0;
let speedMult = $speedScale;

function reset() {
  score = 0;
  gameOver = false;
  player.x = W / 2;
  player.y = H * 0.8;
  obstacles = [];
  particles = [];
  spawnTimer = 0;
}

function handleMove(clientX) {
  if (!gameOver) {
    player.x = Math.max(player.r + 8, Math.min(W - player.r - 8, clientX));
  }
}

canvas.addEventListener('touchstart', e => {
  e.preventDefault();
  if (gameOver) { reset(); return; }
  handleMove(e.touches[0].clientX);
}, { passive: false });

canvas.addEventListener('touchmove', e => {
  e.preventDefault();
  handleMove(e.touches[0].clientX);
}, { passive: false });

canvas.addEventListener('pointerdown', e => {
  if (gameOver) { reset(); return; }
  handleMove(e.clientX);
});

canvas.addEventListener('pointermove', e => {
  if (e.buttons > 0) handleMove(e.clientX);
});

function spawnExplosion(x, y, color) {
  for (let i = 0; i < 24; i++) {
    const a = Math.random() * Math.PI * 2;
    const s = 2 + Math.random() * 6;
    particles.push({ x, y, vx: Math.cos(a) * s, vy: Math.sin(a) * s, life: 1, color });
  }
}

function update() {
  if (!gameOver) {
    score += 0.15;
    spawnTimer++;
    const interval = Math.max(12, Math.floor(32 - score / 25));
    if (spawnTimer >= interval) {
      spawnTimer = 0;
      const size = 22 + Math.random() * 28;
      obstacles.push({
        x: Math.random() * (W - size),
        y: -size,
        w: size,
        h: size,
        vy: (4.2 + Math.random() * 3.5 + score * 0.015) * speedMult
      });
    }

    for (let i = obstacles.length - 1; i >= 0; i--) {
      const o = obstacles[i];
      o.y += o.vy;
      const cx = Math.max(o.x, Math.min(player.x, o.x + o.w));
      const cy = Math.max(o.y, Math.min(player.y, o.y + o.h));
      const dx = player.x - cx;
      const dy = player.y - cy;
      if (dx * dx + dy * dy < player.r * player.r) {
        gameOver = true;
        spawnExplosion(player.x, player.y, '${palette.secondary}');
        if (window.AndroidBridge && window.AndroidBridge.onScoreUpdate) {
          window.AndroidBridge.onScoreUpdate(Math.floor(score));
        }
      }
      if (o.y > H + 50) obstacles.splice(i, 1);
    }
  }

  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i];
    p.x += p.vx;
    p.y += p.vy;
    p.life -= 0.03;
    if (p.life <= 0) particles.splice(i, 1);
  }
}

function draw() {
  ctx.fillStyle = '${palette.bg}';
  ctx.fillRect(0, 0, W, H);

  // Subtle Grid
  ctx.strokeStyle = 'rgba(255,255,255,0.04)';
  ctx.lineWidth = 1;
  for (let x = 0; x < W; x += 40) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }

  // Player
  if (!gameOver) {
    ctx.shadowBlur = 18;
    ctx.shadowColor = '${palette.primary}';
    ctx.fillStyle = '${palette.primary}';
    ctx.beginPath();
    ctx.moveTo(player.x, player.y - player.r - 4);
    ctx.lineTo(player.x - player.r, player.y + player.r);
    ctx.lineTo(player.x + player.r, player.y + player.r);
    ctx.closePath();
    ctx.fill();
  }

  // Obstacles
  ctx.shadowBlur = 14;
  ctx.shadowColor = '${palette.secondary}';
  ctx.fillStyle = '${palette.secondary}';
  for (const o of obstacles) {
    if ('$enemyShape' === 'diamond') {
      ctx.beginPath();
      ctx.moveTo(o.x + o.w / 2, o.y);
      ctx.lineTo(o.x + o.w, o.y + o.h / 2);
      ctx.lineTo(o.x + o.w / 2, o.y + o.h);
      ctx.lineTo(o.x, o.y + o.h / 2);
      ctx.closePath();
      ctx.fill();
    } else {
      ctx.fillRect(o.x, o.y, o.w, o.h);
    }
  }

  // Particles
  for (const p of particles) {
    ctx.globalAlpha = Math.max(0, p.life);
    ctx.fillStyle = p.color;
    ctx.fillRect(p.x, p.y, 5, 5);
  }
  ctx.globalAlpha = 1;
  ctx.shadowBlur = 0;

  // HUD
  ctx.fillStyle = '${palette.accent}';
  ctx.font = 'bold 22px monospace';
  ctx.fillText('SCORE: ' + Math.floor(score), 20, 42);

  if (gameOver) {
    ctx.fillStyle = 'rgba(0,0,0,0.72)';
    ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '${palette.secondary}';
    ctx.font = 'bold 34px monospace';
    ctx.textAlign = 'center';
    ctx.fillText('GAME OVER', W / 2, H / 2 - 24);
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '20px monospace';
    ctx.fillText('SCORE: ' + Math.floor(score), W / 2, H / 2 + 16);
    ctx.fillStyle = '${palette.primary}';
    ctx.font = 'bold 18px monospace';
    ctx.fillText('[ TIPPEN FÜR NEUSTART ]', W / 2, H / 2 + 64);
    ctx.textAlign = 'left';
  }
}

function loop() {
  update();
  draw();
  requestAnimationFrame(loop);
}
loop();
</script>
</body>
</html>
        """.trimIndent()

        return GeneratedGameSpec(
            title = title,
            prompt = customPrompt ?: "Offline Neon Dodger",
            htmlCode = html,
            isOffline = true
        )
    }

    private fun buildBrickBreaker(palette: ColorPalette, customPrompt: String?): GeneratedGameSpec {
        val rows = listOf(4, 5, 6).random()
        val title = customPrompt?.takeIf { it.isNotBlank() } ?: "${palette.name} Breaker"
        val html = """
<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; user-select: none; -webkit-user-select: none; }
  body { background: ${palette.bg}; overflow: hidden; touch-action: none; font-family: monospace; }
  canvas { display: block; width: 100vw; height: 100vh; }
</style>
</head>
<body>
<canvas id="c"></canvas>
<script>
const canvas = document.getElementById('c');
const ctx = canvas.getContext('2d');
let W, H;
function resize() { W = canvas.width = window.innerWidth; H = canvas.height = window.innerHeight; }
window.addEventListener('resize', resize);
resize();

let score = 0;
let gameOver = false;
let paddle = { w: 104, h: 14, x: W / 2 - 52, y: H - 70 };
let ball = { x: W / 2, y: H - 100, r: 9, vx: 4.5, vy: -5.2 };
let bricks = [];

function initBricks() {
  bricks = [];
  const cols = 6;
  const pad = 8;
  const bw = (W - pad * (cols + 1)) / cols;
  const bh = 22;
  for (let r = 0; r < $rows; r++) {
    for (let c = 0; c < cols; c++) {
      bricks.push({
        x: pad + c * (bw + pad),
        y: 75 + r * (bh + pad),
        w: bw,
        h: bh,
        alive: true,
        color: r % 2 === 0 ? '${palette.secondary}' : '${palette.accent}'
      });
    }
  }
}

function reset() {
  score = 0;
  gameOver = false;
  paddle.x = W / 2 - paddle.w / 2;
  ball = { x: W / 2, y: H - 110, r: 9, vx: (Math.random() > 0.5 ? 1 : -1) * 4.5, vy: -5.5 };
  initBricks();
}
initBricks();

function movePaddle(clientX) {
  paddle.x = Math.max(6, Math.min(W - paddle.w - 6, clientX - paddle.w / 2));
}

canvas.addEventListener('touchstart', e => {
  e.preventDefault();
  if (gameOver) { reset(); return; }
  movePaddle(e.touches[0].clientX);
}, { passive: false });

canvas.addEventListener('touchmove', e => {
  e.preventDefault();
  movePaddle(e.touches[0].clientX);
}, { passive: false });

canvas.addEventListener('pointerdown', e => {
  if (gameOver) { reset(); return; }
  movePaddle(e.clientX);
});

canvas.addEventListener('pointermove', e => {
  if (e.buttons > 0) movePaddle(e.clientX);
});

function update() {
  if (gameOver) return;
  ball.x += ball.vx;
  ball.y += ball.vy;

  if (ball.x - ball.r < 0 || ball.x + ball.r > W) ball.vx *= -1;
  if (ball.y - ball.r < 0) ball.vy *= -1;

  if (
    ball.y + ball.r >= paddle.y &&
    ball.y - ball.r <= paddle.y + paddle.h &&
    ball.x >= paddle.x &&
    ball.x <= paddle.x + paddle.w
  ) {
    const hitOffset = (ball.x - (paddle.x + paddle.w / 2)) / (paddle.w / 2);
    ball.vx = hitOffset * 6.5;
    ball.vy = -Math.abs(ball.vy) * 1.02;
  }

  let remaining = 0;
  for (const b of bricks) {
    if (!b.alive) continue;
    remaining++;
    if (
      ball.x + ball.r > b.x &&
      ball.x - ball.r < b.x + b.w &&
      ball.y + ball.r > b.y &&
      ball.y - ball.r < b.y + b.h
    ) {
      b.alive = false;
      ball.vy *= -1;
      score += 10;
      if (window.AndroidBridge && window.AndroidBridge.onScoreUpdate) {
        window.AndroidBridge.onScoreUpdate(score);
      }
    }
  }

  if (remaining === 0) {
    initBricks();
    ball.vy *= 1.08;
  }

  if (ball.y - ball.r > H) {
    gameOver = true;
    if (window.AndroidBridge && window.AndroidBridge.onScoreUpdate) {
      window.AndroidBridge.onScoreUpdate(score);
    }
  }
}

function draw() {
  ctx.fillStyle = '${palette.bg}';
  ctx.fillRect(0, 0, W, H);

  for (const b of bricks) {
    if (!b.alive) continue;
    ctx.shadowBlur = 10;
    ctx.shadowColor = b.color;
    ctx.fillStyle = b.color;
    ctx.fillRect(b.x, b.y, b.w, b.h);
  }

  ctx.shadowBlur = 16;
  ctx.shadowColor = '${palette.primary}';
  ctx.fillStyle = '${palette.primary}';
  ctx.fillRect(paddle.x, paddle.y, paddle.w, paddle.h);

  ctx.beginPath();
  ctx.arc(ball.x, ball.y, ball.r, 0, Math.PI * 2);
  ctx.fill();
  ctx.shadowBlur = 0;

  ctx.fillStyle = '${palette.accent}';
  ctx.font = 'bold 22px monospace';
  ctx.fillText('SCORE: ' + score, 20, 42);

  if (gameOver) {
    ctx.fillStyle = 'rgba(0,0,0,0.75)';
    ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '${palette.secondary}';
    ctx.font = 'bold 34px monospace';
    ctx.textAlign = 'center';
    ctx.fillText('GAME OVER', W / 2, H / 2 - 24);
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '20px monospace';
    ctx.fillText('SCORE: ' + score, W / 2, H / 2 + 16);
    ctx.fillStyle = '${palette.primary}';
    ctx.font = 'bold 18px monospace';
    ctx.fillText('[ TIPPEN FÜR NEUSTART ]', W / 2, H / 2 + 64);
    ctx.textAlign = 'left';
  }
}

function loop() {
  update();
  draw();
  requestAnimationFrame(loop);
}
loop();
</script>
</body>
</html>
        """.trimIndent()

        return GeneratedGameSpec(
            title = title,
            prompt = customPrompt ?: "Offline Brick Breaker",
            htmlCode = html,
            isOffline = true
        )
    }

    private fun buildReflexTapper(palette: ColorPalette, customPrompt: String?): GeneratedGameSpec {
        val title = customPrompt?.takeIf { it.isNotBlank() } ?: "${palette.name} Reflex Tap"
        val html = """
<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; user-select: none; -webkit-user-select: none; }
  body { background: ${palette.bg}; overflow: hidden; touch-action: none; font-family: monospace; }
  canvas { display: block; width: 100vw; height: 100vh; }
</style>
</head>
<body>
<canvas id="c"></canvas>
<script>
const canvas = document.getElementById('c');
const ctx = canvas.getContext('2d');
let W, H;
function resize() { W = canvas.width = window.innerWidth; H = canvas.height = window.innerHeight; }
window.addEventListener('resize', resize);
resize();

let score = 0;
let timeLeft = 25.0;
let gameOver = false;
let target = { x: W / 2, y: H / 2, r: 38 };
let rings = [];

function moveTarget() {
  const margin = 65;
  target.r = Math.max(24, 42 - Math.floor(score / 8));
  target.x = margin + Math.random() * (W - margin * 2);
  target.y = margin + 40 + Math.random() * (H - margin * 2 - 40);
}

function reset() {
  score = 0;
  timeLeft = 25.0;
  gameOver = false;
  rings = [];
  moveTarget();
}
moveTarget();

function handleTap(x, y) {
  if (gameOver) { reset(); return; }
  const dx = x - target.x;
  const dy = y - target.y;
  if (dx * dx + dy * dy <= (target.r + 14) * (target.r + 14)) {
    score += 5;
    timeLeft = Math.min(30, timeLeft + 0.45);
    rings.push({ x: target.x, y: target.y, r: target.r, alpha: 1 });
    moveTarget();
    if (window.AndroidBridge && window.AndroidBridge.onScoreUpdate) {
      window.AndroidBridge.onScoreUpdate(score);
    }
  } else {
    timeLeft = Math.max(0, timeLeft - 1.2);
  }
}

canvas.addEventListener('pointerdown', e => {
  e.preventDefault();
  handleTap(e.clientX, e.clientY);
});

let lastTime = performance.now();
function loop(now) {
  const dt = (now - lastTime) / 1000;
  lastTime = now;

  if (!gameOver) {
    timeLeft -= dt;
    if (timeLeft <= 0) {
      timeLeft = 0;
      gameOver = true;
      if (window.AndroidBridge && window.AndroidBridge.onScoreUpdate) {
        window.AndroidBridge.onScoreUpdate(score);
      }
    }
  }

  ctx.fillStyle = '${palette.bg}';
  ctx.fillRect(0, 0, W, H);

  for (let i = rings.length - 1; i >= 0; i--) {
    const ring = rings[i];
    ring.r += 3;
    ring.alpha -= 0.04;
    if (ring.alpha <= 0) { rings.splice(i, 1); continue; }
    ctx.strokeStyle = '${palette.primary}';
    ctx.globalAlpha = ring.alpha;
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(ring.x, ring.y, ring.r, 0, Math.PI * 2);
    ctx.stroke();
  }
  ctx.globalAlpha = 1;

  if (!gameOver) {
    ctx.shadowBlur = 22;
    ctx.shadowColor = '${palette.secondary}';
    ctx.fillStyle = '${palette.secondary}';
    ctx.beginPath();
    ctx.arc(target.x, target.y, target.r, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '${palette.primary}';
    ctx.beginPath();
    ctx.arc(target.x, target.y, target.r * 0.42, 0, Math.PI * 2);
    ctx.fill();
    ctx.shadowBlur = 0;
  }

  ctx.fillStyle = '${palette.accent}';
  ctx.font = 'bold 22px monospace';
  ctx.fillText('SCORE: ' + score, 20, 42);
  ctx.textAlign = 'right';
  ctx.fillStyle = timeLeft < 5 ? '${palette.secondary}' : '${palette.primary}';
  ctx.fillText('ZEIT: ' + timeLeft.toFixed(1) + 's', W - 20, 42);
  ctx.textAlign = 'left';

  if (gameOver) {
    ctx.fillStyle = 'rgba(0,0,0,0.75)';
    ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '${palette.secondary}';
    ctx.font = 'bold 34px monospace';
    ctx.textAlign = 'center';
    ctx.fillText('ZEIT ABGELAUFEN', W / 2, H / 2 - 24);
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '20px monospace';
    ctx.fillText('SCORE: ' + score, W / 2, H / 2 + 16);
    ctx.fillStyle = '${palette.primary}';
    ctx.font = 'bold 18px monospace';
    ctx.fillText('[ TIPPEN FÜR NEUSTART ]', W / 2, H / 2 + 64);
    ctx.textAlign = 'left';
  }

  requestAnimationFrame(loop);
}
requestAnimationFrame(loop);
</script>
</body>
</html>
        """.trimIndent()

        return GeneratedGameSpec(
            title = title,
            prompt = customPrompt ?: "Offline Reflex Tapper",
            htmlCode = html,
            isOffline = true
        )
    }
}
