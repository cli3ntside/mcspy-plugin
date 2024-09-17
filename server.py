from flask import Flask, request
import discord
import asyncio
import requests

app = Flask(__name__)

DISCORD_TOKEN = 'BOT_TOKEN'
CHAT_CHANNEL_ID = 1250448458285056022
NOTIFICATION_CHANNEL_ID = 1250448458285056022
COMMAND_CHANNEL_ID_SERVER1 = 1250448458285056022
COMMAND_CHANNEL_ID_SERVER2 = 1253728640722800750
COMMAND_CHANNEL_ID_SERVER3 = 1253728640722800750

intents = discord.Intents.default()
client = discord.Client(intents=intents)

@app.route('/receive', methods=['POST'])
def receive():
    data = request.json
    message_type = data.get('type')
    player = data.get('player')
    message = data.get('message')
    server = data.get('server')

    if message_type and player and message:
        asyncio.run_coroutine_threadsafe(
            send_to_discord(message_type, player, message, server),
            client.loop
        )
    return 'OK', 200

async def send_to_discord(message_type, player, message, server):
    if server == "server2":
        channel_id = COMMAND_CHANNEL_ID_SERVER2 if message_type in ['command', 'join'] else NOTIFICATION_CHANNEL_ID
    elif server == "server3":
        channel_id = COMMAND_CHANNEL_ID_SERVER3 if message_type in ['command', 'join'] else NOTIFICATION_CHANNEL_ID
    else:
        channel_id = COMMAND_CHANNEL_ID_SERVER1 if message_type in ['command', 'join'] else NOTIFICATION_CHANNEL_ID

    channel = client.get_channel(channel_id)
    if channel:
        await channel.send(f"[{message_type.upper()}] {player}: {message}")

@client.event
async def on_ready():
    print(f'We have logged in as {client.user}')

def run_flask_app():
    app.run(host='0.0.0.0', port=5000)

if __name__ == "__main__":
    import threading
    threading.Thread(target=run_flask_app).start()
    client.run(DISCORD_TOKEN)