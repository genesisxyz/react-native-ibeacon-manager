import {
  AppRegistry,
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-ibeacon-manager' doesn't seem to be linked. Make sure: \n\n` +
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
  distance?: number;
  major?: number;
  minor?: number;
};

type DeepPartial<T> = {
  [P in keyof T]?: DeepPartial<T[P]>;
};

export type Options = DeepPartial<{
  android: {
    notification: {
      id: number;
      contentTitle: string;
      contentText: string;
      channel: {
        id: string;
        name: string;
        description: string;
      };
      smallIcon: string;
    };
  };
}>;

let listener: ((beacons: Beacon[]) => void) | null = null;

let myModuleEvt: NativeEventEmitter;

if (Platform.OS === 'android') {
  AppRegistry.registerHeadlessTask('Beacons', () => async (data: any) => {
    const { beacons } = data.data;
    listener && listener(beacons);
    return Promise.resolve();
  });
} else {
  myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
  myModuleEvt.addListener('watchBeacons', (event) => {
    listener && listener(event);
  });
}

export default {
  watchBeacons(callback: (beacons: Beacon[]) => void) {
    listener = callback;
  },
  setOptions(options: Options): void {
    Beacon.setOptions(options);
  },
  requestPermissions(): Promise<boolean> {
    return Beacon.requestPermissions();
  },
  enableBluetooth() {
    Beacon.enableBluetooth();
  },
  async startBeaconScan(beacons: BeaconPayload[]) {
    await Beacon.startService();
    Beacon.startBeaconScan(beacons);
  },
  async stopBeaconScan() {
    Beacon.stopBeaconScan();
    await Beacon.stopService();
  },
};
