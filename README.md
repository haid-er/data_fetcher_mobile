# data_fetcher_mobile for MotionIQ

## Screenshots

<p float="left">
	<img src="https://github.com/haid-er/data_fetcher_mobile/imgs/im1.png" width="200"/>
	<img src="https://github.com/haid-er/data_fetcher_mobile/imgs/im2.png" width="200"/>
</p>

#### Simple and Beautiful App for fetching and send sensor's data to server.

### Steps :

> First, you have to set the rabbitmq server's address and port in application.

> Then, select appropriate sensors to send data.

> click start button to send data.

<br/>

##### Note :

> Don't forget to follow the following guide lines to start the application successfully.

<br />

![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg?color=00ADB5&style=for-the-badge)
![Server](https://img.shields.io/badge/server-RabbitMQ-orange.svg?color=FCA311&style=for-the-badge)

## Overview

A step-by-step guide for setting up a **MotionIQ Human Activity Recognition System** using RabbitMQ and an Android app. Data is transmitted from a mobile device to RabbitMQ queues and processed by a Java-based consumer.

---

## Features

- 📱 Real-time data from mobile sensors
- 🧠 Activity recognition pipeline
- 🐇 RabbitMQ message brokering
- 💻 Java consumer backend

---

## Setup Guide

### 🔧 Installing RabbitMQ

1. Download the installer from [RabbitMQ Downloads](https://www.rabbitmq.com/download.html)
2. Install and run the server
3. Enable management UI:

```bash
cd "C:\Program Files\RabbitMQ Server\rabbitmq_server-{version}\sbin"
rabbitmq-plugins enable rabbitmq_management
```

4. Access the UI: [http://localhost:15672](http://localhost:15672)

### 👤 Create a User

- Go to **Admin > Add a user**
- Set:
  - Username: `test`
  - Password: `test`
  - Tags: `administrator`
- Assign permissions

> Note: Guest user can't connect remotely.

### 🖥️ Running Consumer

#### Required Files

- `SensorDataConsumer.java`
- `amqp-client-5.21.0.jar`
- `slf4j-api-2.0.13.jar`
- `slf4j-simple-2.0.13.jar`

#### Steps

```bash
javac SensorDataConsumer.java
java SensorDataConsumer
```

Check RabbitMQ UI under **Connections** tab to verify.

### 🔒 Firewall Setup

Open ports `15672` and `5672` in Windows Firewall.

### 📱 Android App Setup

1. Install the mobile app.
2. Enter:
   - IP address of RabbitMQ server
   - Port: `5672`
   - Username: `test`
3. Select sensors via UI

#### Using Android Studio

1. Extract and open the project
2. Check `sendDataService` file for credentials
3. Run the app on a device with USB/Wi-Fi debugging enabled

### ✅ Testing

1. Run RabbitMQ server and consumer
2. Start app and begin data transmission
3. Monitor logs and RabbitMQ Queues tab for activity

---

## Contributing

All contributions are welcome! Feel free to fork, raise issues, or submit PRs.
