import CoreLocation
import UserNotifications

@objc(Beacon)
class Beacon: NSObject, CLLocationManagerDelegate {

  var locationManager: CLLocationManager? = nil
  
  var backgroundTask: UIBackgroundTaskIdentifier = UIBackgroundTaskIdentifier.invalid
  
  func initializeLocationManager() -> CLLocationManager {
    let manager = locationManager ?? CLLocationManager()
    manager.delegate = self
    manager.allowsBackgroundLocationUpdates = true
    manager.pausesLocationUpdatesAutomatically = false
    manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
    manager.distanceFilter = 3000.0
    return manager
  }
  
  @objc(initialize:withRejecter:)
  func initialize(resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) -> Void {
    syncMain {
      locationManager = initializeLocationManager();
      locationManager?.startUpdatingLocation()
      
      // keeps the app open for ranging beacons
      // TODO: disabled because when React hot reloads it keeps creating new threads
      // infiniteTask()
    }
    resolve(true)
  }
  
  @objc(requestPermissions:withRejecter:)
  func requestPermissions(resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) -> Void {
    locationManager?.requestAlwaysAuthorization()
    resolve(true)
  }
  
  @objc(startBeaconScan:)
  func startBeaconScan(beacons: Array<[String: AnyObject]>) -> Void {
    beacons.forEach { beacon in

      let id = beacon["id"] as! String
      let uuid = beacon["uuid"] as! String
      let major = beacon["major"] as? Double
      let minor = beacon["minor"] as? Double
      
      var region: CLBeaconRegion;
      
      if (major == nil && minor == nil) {
        region = CLBeaconRegion(uuid: UUID(uuidString: uuid)!, identifier: id)
      } else if (minor == nil) {
        region = CLBeaconRegion(uuid: UUID(uuidString: uuid)!, major: CLBeaconMajorValue(major!), identifier: id)
      } else {
        let constraint = CLBeaconIdentityConstraint(uuid: UUID(uuidString: uuid)!, major: CLBeaconMajorValue(major!), minor: CLBeaconMajorValue(minor!))
        region = CLBeaconRegion(beaconIdentityConstraint: constraint, identifier: id)
      }
      
      region.notifyOnExit = true
      region.notifyOnEntry = true
      region.notifyEntryStateOnDisplay = true
      
      if CLLocationManager.isMonitoringAvailable(for: CLBeaconRegion.self) {
        locationManager?.startMonitoring(for: region)
        locationManager?.requestState(for: region)
      }
    }
  }
  
  @objc(stopBeaconScan)
  func stopBeaconScan() -> Void {
    locationManager?.rangedRegions.forEach { region in
      locationManager?.stopRangingBeacons(in: region as! CLBeaconRegion)
    }

    locationManager?.monitoredRegions.forEach { region in
      locationManager?.stopMonitoring(for: region)
    }
  }
  
  @objc(enableBluetooth)
  func enableBluetooth() -> Void {
    
  }
  
  @objc(setOptions:)
  func setOptions(options: [String: AnyObject]) -> Void {
    
  }
  
  // http://www.davidgyoungtech.com/2023/02/10/forever-ranging
  private func infiniteTask() {
    if (self.backgroundTask != UIBackgroundTaskIdentifier.invalid) {
      return;
    }
    
      NSLog("Attempting to extend background running time")
          
      self.backgroundTask = UIApplication.shared.beginBackgroundTask(withName: "DummyTask", expirationHandler: {
        NSLog("Background task expired by iOS.")
        UIApplication.shared.endBackgroundTask(self.backgroundTask)
      })

        
      var lastLogTime = 0.0
      DispatchQueue.global().async {
        let startedTime = Int(Date().timeIntervalSince1970) % 10000000
        NSLog("*** STARTED BACKGROUND THREAD")
        while(true) {
            DispatchQueue.main.async {
                let now = Date().timeIntervalSince1970
                let backgroundTimeRemaining = UIApplication.shared.backgroundTimeRemaining
                if abs(now - lastLogTime) >= 2.0 {
                    lastLogTime = now
                    if backgroundTimeRemaining < 10.0 {
                      NSLog("About to suspend based on background thread running out.")
                    }
                    if (backgroundTimeRemaining < 200000.0) {
                     NSLog("Thread \(startedTime) background time remaining: \(backgroundTimeRemaining)")
                    }
                    else {
                      //NSLog("Thread \(startedTime) background time remaining: INFINITE")
                    }
                }
            }
            sleep(1)
        }
      }
  }

  
  // MARK: CLLocationManagerDelegate
  
  public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
    
  }
  
  @available(iOS 14.0, *)
  public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
    let status = manager.authorizationStatus
    
  }

  public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    
  }
  
  public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    
  }

  public func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
    if region is CLBeaconRegion {
      if CLLocationManager.isRangingAvailable() {
        manager.startRangingBeacons(in: region as! CLBeaconRegion)
      }
    }
  }

  public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
    if region is CLBeaconRegion {
      if CLLocationManager.isRangingAvailable() {
        manager.stopRangingBeacons(in: region as! CLBeaconRegion)
      }
    }

  }

  public func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for region: CLRegion) {
    switch (state) {
    case CLRegionState.inside:
      if region is CLBeaconRegion {
        if CLLocationManager.isRangingAvailable() {
          manager.startRangingBeacons(in: region as! CLBeaconRegion)
        }
      }
    case CLRegionState.outside:
      if region is CLBeaconRegion {
        if CLLocationManager.isRangingAvailable() {
          manager.stopRangingBeacons(in: region as! CLBeaconRegion)
        }
      }
    default:
      break
    }
  }
  
  func locationManager(_ manager: CLLocationManager, didRangeBeacons beacons: [CLBeacon], in region: CLBeaconRegion) {
    let beaconsDict = beacons.filter({ beacon in
      beacon.accuracy > -1
    }).map { beacon in
      [
        "uuid": beacon.uuid.uuidString,
        "distance": beacon.accuracy,
        "major": beacon.major,
        "minor": beacon.minor,
      ]
    }
    
    if (beaconsDict.isEmpty) {
      return
    }
    
    MyEventEmitter.shared?.watchBeacons(beacons: beaconsDict)
  }
}

func syncMain<T>(_ closure: () -> T) -> T {
    if Thread.isMainThread {
        return closure()
    } else {
        return DispatchQueue.main.sync(execute: closure)
    }
}
