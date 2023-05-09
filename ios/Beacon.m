#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(Beacon, NSObject)

RCT_EXTERN_METHOD(initialize:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(requestPermissions:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(startBeaconScan:(NSArray<NSDictionary *> *)beacons)

RCT_EXTERN_METHOD(stopBeaconScan)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
