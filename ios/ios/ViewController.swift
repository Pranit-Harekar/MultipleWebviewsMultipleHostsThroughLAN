//
//  ViewController.swift
//  ios
//
//  Created by Pranit Harekar on 13/01/23.
//

import UIKit
import CocoaAsyncSocket
import Alamofire
import WebKit

class ViewController: UIViewController, GCDAsyncUdpSocketDelegate, WKScriptMessageHandler {

    static let queue = DispatchQueue(label: "com.pranitharekar.CFDDetector", attributes: [])

    let broadcastPort: UInt16 = 9997
    let broadcastHost: String = "255.255.255.255"
    let broadcastTimeout = 1.0
    let disconnectAfter = 1.0
    var socket: GCDAsyncUdpSocket!
    var stopDiscovery = false
    var address: String = ""

    var browserPlugin = "browserPlugin"

    @IBOutlet weak var CFDLabel: UILabel!
    @IBOutlet weak var webview: WKWebView!

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

        self.webview.isHidden = true
        self.loadInitialPage()
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
                self.webview.isHidden = false
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

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == browserPlugin {
            let parameters: [String: AnyObject] = [
                "data": message.body as AnyObject
            ]

            AF.request("http://\(address):8080/cfd", method: .post, parameters: parameters).response { response in
                debugPrint(response)
            }

        }}

    private func loadInitialPage() {
        do {
            guard let filePath = Bundle.main.path(forResource: "app1", ofType: "html") else {
                print("File reading error")
                return
            }

            let contents =  try String(contentsOfFile: filePath, encoding: .utf8)
            let baseUrl = URL(fileURLWithPath: filePath)
            webview.loadHTMLString(contents as String, baseURL: baseUrl)
            webview.configuration.preferences.javaScriptEnabled = true

            let js: String = "const bc2 = new BroadcastChannel(\"app_1\"); bc2.onmessage = (event) => { window.webkit.messageHandlers.\(browserPlugin).postMessage(event.data);};"
            let script = WKUserScript(source: js, injectionTime: .atDocumentEnd, forMainFrameOnly: false)
            webview.configuration.userContentController.addUserScript(script)

            webview.configuration.userContentController = WKUserContentController()
            webview.configuration.userContentController.add(self, name: browserPlugin)
        }
        catch {
            print("File HTML error")
        }
    }
}

