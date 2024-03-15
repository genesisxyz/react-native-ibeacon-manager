//
//  MyEventEmitter.swift
//  react-native-ibeacon-manager
//
//  Created by Damiano Collu on 08/05/23.
//

import Foundation
import React

@objc(MyEventEmitter)
class MyEventEmitter: RCTEventEmitter {

  public static var shared: MyEventEmitter?
  
  private var hasListeners = false

  override init() {
    super.init()
    MyEventEmitter.shared = self
  }
  
  @objc override static func requiresMainQueueSetup() -> Bool {
    return false
  }

  override func supportedEvents() -> [String]! {
    return [
      "watchBeacons",
    ]
  }
  
  override func startObserving() {
    hasListeners = true
  }
  
  override func stopObserving() {
    hasListeners = false
  }
  
  func watchBeacons(beacons: Array<[String: Any?]>) {
    if (hasListeners) {
      self.sendEvent(withName: "watchBeacons", body: beacons)
    }
  }
  
  override func removeListeners(_ count: Double) {
    NSLog("removeListeners")
  }
}
