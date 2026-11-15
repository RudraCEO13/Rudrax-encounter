package com.example.runtime

object MotionXSdkTemplates {

    val MOTIONX_SDK_JS = """
        (function(global) {
            if (global.MotionX && global.MotionX.__initialized) return;

            const listeners = {
                handMove: [],
                gesture: [],
                airDraw: [],
                connection: [],
                objectAction: []
            };

            let config = {
                version: "2.0.0",
                sensitivity: 1.0,
                mappings: {}
            };

            let ws = null;
            let connectionStatus = {
                status: "CONNECTED",
                deviceName: "Local Runtime",
                pin: "",
                latencyMs: 8,
                transport: "Bridge"
            };

            function emit(channel, payload) {
                const list = listeners[channel] || [];
                for (let i = 0; i < list.length; i++) {
                    try {
                        list[i](payload);
                    } catch (err) {
                        console.error("[MotionX] Listener error on " + channel, err);
                    }
                }
            }

            function triggerMappedKey(keyCode) {
                if (!keyCode || keyCode === "Draw" || keyCode === "Equip") return;
                const downEv = new KeyboardEvent("keydown", { key: keyCode, code: keyCode, bubbles: true });
                const upEv = new KeyboardEvent("keyup", { key: keyCode, code: keyCode, bubbles: true });
                document.dispatchEvent(downEv);
                window.dispatchEvent(downEv);
                setTimeout(() => {
                    document.dispatchEvent(upEv);
                    window.dispatchEvent(upEv);
                }, 70);
            }

            function connectWebSocket(wsUrl, pin) {
                if (!wsUrl) return;
                try {
                    if (ws) ws.close();
                    ws = new WebSocket(wsUrl);
                    ws.onopen = function() {
                        ws.send(JSON.stringify({ type: "auth", pin: pin || "" }));
                        connectionStatus = {
                            status: "CONNECTED",
                            deviceName: "TV / External Display",
                            pin: pin || "",
                            latencyMs: 6,
                            transport: "Wi-Fi WebSocket"
                        };
                        emit("connection", connectionStatus);
                    };
                    ws.onmessage = function(event) {
                        try {
                            const msg = JSON.parse(event.data);
                            handleIncomingMessage(msg);
                        } catch (e) {}
                    };
                    ws.onclose = function() {
                        connectionStatus = Object.assign({}, connectionStatus, { status: "DISCONNECTED" });
                        emit("connection", connectionStatus);
                    };
                    ws.onerror = function() {
                        connectionStatus = Object.assign({}, connectionStatus, { status: "ERROR" });
                        emit("connection", connectionStatus);
                    };
                } catch (e) {
                    console.warn("[MotionX] WS connect failed", e);
                }
            }

            function handleIncomingMessage(msg) {
                if (!msg || !msg.channel) return;
                if (msg.channel === "handMove") {
                    emit("handMove", msg.payload);
                } else if (msg.channel === "gesture") {
                    emit("gesture", msg.payload);
                    if (msg.payload && msg.payload.mappedControl) {
                        triggerMappedKey(msg.payload.mappedControl);
                    }
                } else if (msg.channel === "airDraw") {
                    emit("airDraw", msg.payload);
                } else if (msg.channel === "connection") {
                    connectionStatus = msg.payload;
                    emit("connection", msg.payload);
                } else if (msg.channel === "objectAction") {
                    emit("objectAction", msg.payload);
                }
            }

            const MotionX = {
                __initialized: true,
                version: "2.0.0",
                onHandMove: function(cb) {
                    if (typeof cb === "function") listeners.handMove.push(cb);
                },
                onGesture: function(cb) {
                    if (typeof cb === "function") listeners.gesture.push(cb);
                },
                onAirDraw: function(cb) {
                    if (typeof cb === "function") listeners.airDraw.push(cb);
                },
                onConnection: function(cb) {
                    if (typeof cb === "function") {
                        listeners.connection.push(cb);
                        cb(connectionStatus);
                    }
                },
                onObjectAction: function(cb) {
                    if (typeof cb === "function") listeners.objectAction.push(cb);
                },
                getConfig: function() {
                    return config;
                },
                setMapping: function(mapping) {
                    config.mappings = Object.assign({}, config.mappings, mapping || {});
                    if (global.MotionXAndroidBridge && global.MotionXAndroidBridge.onConfigUpdate) {
                        global.MotionXAndroidBridge.onConfigUpdate(JSON.stringify(config));
                    }
                },
                sendFeedback: function(event) {
                    if (global.MotionXAndroidBridge && global.MotionXAndroidBridge.onGameFeedback) {
                        global.MotionXAndroidBridge.onGameFeedback(JSON.stringify(event || {}));
                    }
                },
                __dispatch: function(channel, payload) {
                    handleIncomingMessage({ channel: channel, payload: payload });
                },
                __connectRemote: connectWebSocket
            };

            global.MotionX = MotionX;

            // Auto-connect if launched on TV/Laptop Receiver with ?ws=...&pin=...
            const params = new URLSearchParams(global.location.search);
            const wsParam = params.get("ws");
            const pinParam = params.get("pin");
            if (wsParam) {
                connectWebSocket(wsParam, pinParam || "");
            }
        })(window);
    """.trimIndent()

    fun buildReceiverHtml(wsPort: Int, defaultGameId: String?): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>RUDRAX MOTION X • TV / Display Receiver</title>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    background: #04060A; color: #F8FAFC;
                    font-family: system-ui, -apple-system, sans-serif;
                    height: 100vh; display: flex; flex-direction: column; overflow: hidden;
                }
                header {
                    display: flex; align-items: center; justify-content: space-between;
                    padding: 12px 20px; background: rgba(14,20,34,0.92);
                    border-bottom: 1px solid rgba(0,240,255,0.28);
                }
                .brand { font-weight: 800; letter-spacing: 1.5px; color: #00F0FF; }
                .status { font-size: 13px; color: #94A3B8; display: flex; gap: 16px; align-items: center; }
                .pill {
                    padding: 4px 10px; border-radius: 999px;
                    background: rgba(0,240,255,0.14); border: 1px solid rgba(0,240,255,0.45);
                    color: #00F0FF; font-family: monospace; font-size: 12px;
                }
                #stage { position: relative; flex: 1; background: #000; }
                iframe { width: 100%; height: 100%; border: none; }
                #pinGate {
                    position: absolute; inset: 0; background: rgba(4,6,10,0.96);
                    display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px;
                }
                input {
                    background: #0E1422; border: 1px solid #00F0FF; color: #FFF;
                    padding: 12px 18px; border-radius: 10px; font-size: 20px;
                    letter-spacing: 6px; text-align: center; width: 220px;
                }
                button {
                    background: #00F0FF; color: #04060A; border: none; padding: 12px 26px;
                    border-radius: 10px; font-weight: 700; cursor: pointer;
                }
            </style>
        </head>
        <body>
            <header>
                <div class="brand">RUDRAX MOTION X • EXTERNAL RECEIVER</div>
                <div class="status">
                    <span id="connStatus" class="pill">AWAITING PIN</span>
                    <span id="gestureHud">Gesture: --</span>
                    <span id="handsHud">Hands: 0</span>
                </div>
            </header>
            <div id="stage">
                <iframe id="gameFrame" sandbox="allow-scripts allow-same-origin"></iframe>
                <div id="pinGate">
                    <h2>Pair TV / Display Receiver</h2>
                    <p style="color:#94A3B8;">Enter the 6-digit PIN shown on your Android Motion X controller</p>
                    <input id="pinInput" maxlength="6" placeholder="000000" />
                    <button onclick="connectReceiver()">CONNECT DISPLAY</button>
                    <div id="errMsg" style="color:#EF4444;font-size:13px;"></div>
                </div>
            </div>
            <script>
                const params = new URLSearchParams(window.location.search);
                const prePin = params.get("pin") || "";
                const preGame = params.get("game") || "${defaultGameId ?: ""}";
                if (prePin) document.getElementById("pinInput").value = prePin;

                let ws = null;
                function connectReceiver() {
                    const pin = document.getElementById("pinInput").value.trim();
                    const host = window.location.hostname;
                    const wsUrl = "ws://" + host + ":$wsPort";
                    ws = new WebSocket(wsUrl);
                    ws.onopen = () => {
                        ws.send(JSON.stringify({ type: "auth", pin: pin }));
                    };
                    ws.onmessage = (ev) => {
                        const msg = JSON.parse(ev.data);
                        if (msg.type === "auth_ok") {
                            document.getElementById("pinGate").style.display = "none";
                            document.getElementById("connStatus").textContent = "PAIRED • LIVE";
                            const gameId = msg.activeGameId || preGame;
                            const entry = msg.entryPoint || "index.html";
                            if (gameId) {
                                document.getElementById("gameFrame").src =
                                    "/games/" + gameId + "/" + entry + "?ws=" + encodeURIComponent(wsUrl) + "&pin=" + encodeURIComponent(pin);
                            }
                        } else if (msg.type === "auth_error") {
                            document.getElementById("errMsg").textContent = "Invalid Pairing PIN";
                        } else if (msg.type === "load_game") {
                            document.getElementById("gameFrame").src =
                                "/games/" + msg.gameId + "/" + msg.entryPoint + "?ws=" + encodeURIComponent(wsUrl) + "&pin=" + encodeURIComponent(pin);
                        } else if (msg.channel === "handMove") {
                            const count = (msg.payload.leftHand ? 1 : 0) + (msg.payload.rightHand ? 1 : 0);
                            document.getElementById("handsHud").textContent = "Hands: " + count;
                        } else if (msg.channel === "gesture") {
                            document.getElementById("gestureHud").textContent = "Gesture: " + (msg.payload.name || "--");
                        }
                    };
                }
                if (prePin.length === 6) connectReceiver();
            </script>
        </body>
        </html>
    """.trimIndent()

    /**
     * Real playable HTML5/Canvas Cyber Arena game powered by window.MotionX SDK.
     * Supports dual-hand shields/blasters, air-drawn energy barriers, and equipped 3D virtual weapons.
     */
    val CYBER_ARENA_INDEX_HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Cyber Motion Arena • MotionX SDK Game</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { background: #030509; color: #F8FAFC; font-family: system-ui, sans-serif; overflow: hidden; }
                canvas { display: block; width: 100vw; height: 100vh; }
                #hud {
                    position: fixed; top: 12px; left: 12px; right: 12px;
                    display: flex; justify-content: space-between; pointer-events: none;
                    font-family: monospace; font-size: 13px;
                }
                .badge {
                    background: rgba(9, 13, 22, 0.8); border: 1px solid rgba(0, 240, 255, 0.45);
                    padding: 6px 12px; border-radius: 999px; color: #00F0FF;
                }
            </style>
        </head>
        <body>
            <div id="hud">
                <div class="badge" id="scoreBadge">SCORE: 0 • ARENA LIVE</div>
                <div class="badge" id="weaponBadge">WEAPON: PULSE DEFAULT</div>
                <div class="badge" id="gestureBadge">GESTURE: READY</div>
            </div>
            <canvas id="arena"></canvas>
            <script src="/sdk/motionx-sdk.js"></script>
            <script>
                const canvas = document.getElementById("arena");
                const ctx = canvas.getContext("2d");
                let W = canvas.width = window.innerWidth;
                let H = canvas.height = window.innerHeight;
                window.addEventListener("resize", () => {
                    W = canvas.width = window.innerWidth;
                    H = canvas.height = window.innerHeight;
                });

                const player = { x: W * 0.5, y: H * 0.8, shield: false, weapon: "Pulse Blaster", score: 0 };
                let leftCursor = null;
                let rightCursor = null;
                let airLines = [];
                let targets = [];
                let bolts = [];

                function spawnTarget() {
                    targets.push({
                        x: 60 + Math.random() * (W - 120),
                        y: -25,
                        vy: 1.4 + Math.random() * 1.8,
                        r: 18 + Math.random() * 12,
                        hue: Math.random() > 0.5 ? "#00F0FF" : "#8B5CF6"
                    });
                }
                setInterval(spawnTarget, 1100);

                function fireBolt(x, y) {
                    bolts.push({ x, y, vy: -11 });
                }

                if (window.MotionX) {
                    MotionX.onHandMove((data) => {
                        if (data.rightHand && data.rightHand.palmCenter) {
                            rightCursor = { x: data.rightHand.palmCenter.x * W, y: data.rightHand.palmCenter.y * H };
                            player.x += (rightCursor.x - player.x) * 0.25;
                            player.y += (rightCursor.y - player.y) * 0.22;
                        }
                        if (data.leftHand && data.leftHand.palmCenter) {
                            leftCursor = { x: data.leftHand.palmCenter.x * W, y: data.leftHand.palmCenter.y * H };
                            player.shield = !!data.leftHand.isOpenPalm;
                        }
                    });

                    MotionX.onGesture((g) => {
                        document.getElementById("gestureBadge").textContent = "GESTURE: " + g.name;
                        if (g.name === "PINCH" || g.name === "CLOSED_FIST" || g.name === "PUSH_FORWARD") {
                            fireBolt(player.x, player.y - 24);
                        }
                        if (g.name === "MOVE_LEFT") player.x = Math.max(30, player.x - 36);
                        if (g.name === "MOVE_RIGHT") player.x = Math.min(W - 30, player.x + 36);
                        if (g.name === "RAISE_HAND") player.y = Math.max(60, player.y - 32);
                        if (g.name === "LOWER_HAND") player.y = Math.min(H - 40, player.y + 32);
                    });

                    MotionX.onAirDraw((drawing) => {
                        if (drawing && drawing.points) {
                            airLines = drawing.points;
                        }
                    });

                    MotionX.onObjectAction((obj) => {
                        player.weapon = obj.name || "Cyber Asset";
                        document.getElementById("weaponBadge").textContent = "EQUIPPED: " + player.weapon.toUpperCase();
                    });
                }

                // Keyboard compatibility too
                window.addEventListener("keydown", (e) => {
                    if (e.code === "ArrowLeft") player.x -= 24;
                    if (e.code === "ArrowRight") player.x += 24;
                    if (e.code === "Space" || e.code === "Enter") fireBolt(player.x, player.y - 24);
                });

                function loop() {
                    ctx.fillStyle = "rgba(3, 5, 9, 0.35)";
                    ctx.fillRect(0, 0, W, H);

                    // Subtle cyber grid
                    ctx.strokeStyle = "rgba(0, 240, 255, 0.06)";
                    ctx.lineWidth = 1;
                    for (let x = 0; x < W; x += 48) {
                        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke();
                    }

                    // Air-drawn energy trails
                    if (airLines.length > 1) {
                        ctx.strokeStyle = "#EC4899";
                        ctx.lineWidth = 4;
                        ctx.beginPath();
                        airLines.forEach((p, idx) => {
                            const px = p.x * W, py = p.y * H;
                            if (idx === 0) ctx.moveTo(px, py); else ctx.lineTo(px, py);
                        });
                        ctx.stroke();
                    }

                    // Bolts
                    bolts = bolts.filter(b => b.y > -20);
                    ctx.fillStyle = "#00F0FF";
                    bolts.forEach(b => {
                        b.y += b.vy;
                        ctx.fillRect(b.x - 3, b.y - 12, 6, 18);
                    });

                    // Targets
                    targets = targets.filter(t => {
                        t.y += t.vy;
                        for (let i = bolts.length - 1; i >= 0; i--) {
                            const dx = bolts[i].x - t.x, dy = bolts[i].y - t.y;
                            if (dx*dx + dy*dy < (t.r + 10) * (t.r + 10)) {
                                bolts.splice(i, 1);
                                player.score += 100;
                                document.getElementById("scoreBadge").textContent = "SCORE: " + player.score + " • ARENA LIVE";
                                return false;
                            }
                        }
                        ctx.strokeStyle = t.hue;
                        ctx.lineWidth = 2.5;
                        ctx.beginPath();
                        ctx.arc(t.x, t.y, t.r, 0, Math.PI * 2);
                        ctx.stroke();
                        return t.y < H + 30;
                    });

                    // Player craft / reticle
                    ctx.strokeStyle = "#00F0FF";
                    ctx.lineWidth = 3;
                    ctx.beginPath();
                    ctx.moveTo(player.x, player.y - 22);
                    ctx.lineTo(player.x - 18, player.y + 14);
                    ctx.lineTo(player.x + 18, player.y + 14);
                    ctx.closePath();
                    ctx.stroke();

                    if (player.shield && leftCursor) {
                        ctx.strokeStyle = "#2563EB";
                        ctx.lineWidth = 4;
                        ctx.beginPath();
                        ctx.arc(leftCursor.x, leftCursor.y, 44, 0, Math.PI * 2);
                        ctx.stroke();
                    }

                    requestAnimationFrame(loop);
                }
                loop();
            </script>
        </body>
        </html>
    """.trimIndent()
}
