import CoreLocation
import UserNotifications

@objc(Beacon)
class Beacon: NSObject, CLLocationManagerDelegate {

  var locationManager: CLLocationManager? = nil
  
  func initializeLocationManager() -> CLLocationManager {
      let manager = locationManager ?? CLLocationManager()
      manager.delegate = self
      manager.allowsBackgroundLocationUpdates = true
      manager.pausesLocationUpdatesAutomatically = false
      return manager
  }

  
  @objc(initialize:withRejecter:)
  func initialize(resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) -> Void {
    syncMain {
      locationManager = initializeLocationManager();
      
      let center = UNUserNotificationCenter.current()
      center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
          
        if let error = error {
            // Handle the error here.
        }
        
        // Enable or disable features based on the authorization.
      }
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
      let major = beacon["major"] as! Double
      let minor = beacon["minor"] as! Double
      
      let region = CLBeaconRegion(proximityUUID: UUID(uuidString: uuid)!, major: CLBeaconMajorValue(major), minor: CLBeaconMajorValue(minor), identifier: id)
      
      region.notifyOnExit = true
      region.notifyOnEntry = true
      region.notifyEntryStateOnDisplay = true
      
      if CLLocationManager.isMonitoringAvailable(for: CLBeaconRegion.self) {
        locationManager?.startMonitoring(for: region)
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
        "uuid": beacon.proximityUUID.uuidString,
        "distance": beacon.accuracy,
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
