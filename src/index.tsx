import {
  AppRegistry,
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-beacon' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const Beacon = NativeModules.Beacon
  ? NativeModules.Beacon
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type BeaconPayload = {
  id: string;
  uuid: string;
  minor?: number;
  major?: number;
};

type Beacon = {
  uuid: string;
  distance: number;
};

let watchBeaconsCallback: (beacons: Beacon[]) => void = () => {};

if (Platform.OS === 'android') {
  AppRegistry.registerHeadlessTask('Beacons', () => async (beacons) => {
    watchBeaconsCallback!(beacons);
    return Promise.resolve();
  });
} else if (Platform.OS === 'ios') {
  const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
  myModuleEvt.addListener('watchBeacons', (beacons) =>
    watchBeaconsCallback(beacons)
  );
}

export default {
  async init(options: {
    registerBeaconsTask: (beacons: Beacon[]) => void;
  }): Promise<boolean> {
    if (await Beacon.initialize()) {
      watchBeaconsCallback = options.registerBeaconsTask;
      return true;
    }
    return false;
  },
  requestPermissions(): Promise<boolean> {
    return Beacon.requestPermissions();
  },
  enableBluetooth() {
    Beacon.enableBluetooth();
  },
  startBeaconScan(beacons: BeaconPayload[]) {
    Beacon.startBeaconScan(beacons);
  },
  stopBeaconScan() {
    Beacon.stopBeaconScan();
  },
};
