#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(Beacon, NSObject)

RCT_EXTERN_METHOD(startService:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(stopService:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(requestPermissions:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(startBeaconScan:(NSArray<NSDictionary *> *)beacons)

RCT_EXTERN_METHOD(stopBeaconScan)

RCT_EXTERN_METHOD(enableBluetooth)

RCT_EXTERN_METHOD(setOptions:(NSDictionary *)options)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
