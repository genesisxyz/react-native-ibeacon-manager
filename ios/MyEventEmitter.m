//
//  MyEventEmitter.m
//  react-native-ibeacon-manager
//
//  Created by Damiano Collu on 08/05/23.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(MyEventEmitter, RCTEventEmitter)

RCT_EXTERN_METHOD(supportedEvents)

@end
