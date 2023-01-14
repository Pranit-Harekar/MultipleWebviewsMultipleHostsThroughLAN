//
//  ViewController.swift
//  ios
//
//  Created by Pranit Harekar on 13/01/23.
//

import UIKit
import CocoaAsyncSocket
import Alamofire

class ViewController: UIViewController, GCDAsyncUdpSocketDelegate, UITextFieldDelegate {

    static let queue = DispatchQueue(label: "com.pranitharekar.CFDDetector", attributes: [])

    let broadcastPort: UInt16 = 9997
    let broadcastHost: String = "255.255.255.255"
    let broadcastTimeout = 1.0
    let disconnectAfter = 1.0
    var socket: GCDAsyncUdpSocket!
    var stopDiscovery = false
    var address: String = ""

    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var CFDLabel: UILabel!

    let udpSocketReceiveFilter: GCDAsyncUdpSocketReceiveFilterBlock = { (data, _, _) -> Bool in
        let message = String(data: data, encoding: .utf8)
        guard let unwrappedMessage = message,
              unwrappedMessage.contains("is at") else {
            print("UDP socket receive filter: Discarding message - \(String(describing: message))")
            return false
        }

        let messageArray = unwrappedMessage.components(separatedBy: "is at")
        return messageArray.count > 1 && messageArray[1].trimmingCharacters(in: .whitespaces).isValidIpAddress()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        self.findCFD()
        self.textField.isHidden = true
        self.textField.delegate = self
    }

    func findCFD() {
        send("Who has CFD")
        DispatchQueue.global(qos: .background).asyncAfter(deadline: .now() + disconnectAfter) {
            if !self.stopDiscovery {
                self.findCFD()
            }
        }
    }

    func send(_ message: String) {
        do {
            // close previous connections if any
            if socket != nil && !socket.isClosed() {
                socket.close()
            }

            // create new socket
            socket = GCDAsyncUdpSocket(
                delegate: self,
                delegateQueue: ViewController.queue,
                socketQueue: ViewController.queue)

            try socket.enableReusePort(true)
            // bind to port so we can listen to incoming data
            try socket.bind(toPort: broadcastPort)
            // enable broadcast so we can send data
            try socket.enableBroadcast(true)
            // enabled "continuous" receiving of the packets
            try socket.beginReceiving()
            // set receive filter
            socket.setReceiveFilter(udpSocketReceiveFilter, with: ViewController.queue)

            let data = message.data(using: .utf8)!
            socket.send(data,
                        toHost: broadcastHost,
                        port: broadcastPort,
                        withTimeout: broadcastTimeout,
                        tag: 0)
        } catch {
            print("Error sending UDP datagram \(error)")
        }
    }


    func extractCFDIPAddress(_ message: String) -> String? {
        let messageArray = message.components(separatedBy: "is at")
        let ip = messageArray[1].trimmingCharacters(in: .whitespaces)
        return ip.isEmpty ? nil : ip
    }

    // MARK: - GCDAsyncUdpSocketDelegate -
    func udpSocket(_ sock: GCDAsyncUdpSocket, didConnectToAddress address: Data) {
        let host = GCDAsyncUdpSocket.host(fromAddress: address) ?? "unknown"
        print("UDP socket connected to \(host) address")
    }

    func udpSocket(_ sock: GCDAsyncUdpSocket, didNotConnect error: Error?) {
        print("UDP socket failed to connect. Error \(error.debugDescription)")
    }

    func udpSocket(_ sock: GCDAsyncUdpSocket, didNotSendDataWithTag tag: Int, dueToError error: Error?) {
        print("UDP socket failed to send data. Error \(error.debugDescription)")
    }

    func udpSocket(_ sock: GCDAsyncUdpSocket, didReceive data: Data, fromAddress address: Data, withFilterContext filterContext: Any?) {
        let parsedData = String(data: data, encoding: .utf8)
        print("UDP socket received data \(String(describing: parsedData))")

        if let message = parsedData,
           let address = extractCFDIPAddress(message) {
            DispatchQueue.main.async{
                self.address = address
                self.CFDLabel.text = "CFD is connected at \(address)"
                self.textField.isHidden = false
            }

            stopDiscovery = true
            socket.close()
        }
    }

    func udpSocket(_ sock: GCDAsyncUdpSocket, didSendDataWithTag tag: Int) {
        print("UDP socket did send data with tag \(tag)")
    }

    func udpSocketDidClose(_ sock: GCDAsyncUdpSocket, withError error: Error?) {
        print("UDP socket closed. Error \(error.debugDescription)")
    }


    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        let parameters: [String: AnyObject] = [
            "data": self.textField.text as AnyObject
        ]

        AF.request("http://\(address):8080/cfd", method: .post, parameters: parameters).response { response in
            debugPrint(response)
        }

        return true
    }
}

