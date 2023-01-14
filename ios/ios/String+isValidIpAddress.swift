//
//  String+isValidIpAddress.swift
//  ios
//
//  Created by Pranit Harekar on 13/01/23.
//

import Foundation

extension String {
    func isValidIpAddress() -> Bool {
        var sin = sockaddr_in()
        var sin6 = sockaddr_in6()

        let isValidIPv6 =  self.withCString({ cstring in inet_pton(AF_INET6, cstring, &sin6.sin6_addr) }) == 1
        let isValidIPv4 =  self.withCString({ cstring in inet_pton(AF_INET, cstring, &sin.sin_addr) }) == 1

        return isValidIPv4 || isValidIPv6
    }
}
