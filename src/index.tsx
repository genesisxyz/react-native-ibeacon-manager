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
  major: number;
  minor: number;
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

let listeners: ((beacons: Beacon[]) => void)[] = [];

let myModuleEvt: NativeEventEmitter;

if (Platform.OS === 'android') {
  AppRegistry.registerHeadlessTask('Beacons', () => async (data: any) => {
    const { beacons } = data.data;
    listeners.forEach((listener) => listener(beacons));
    return Promise.resolve();
  });
} else {
  myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
}

export default {
  async init(): Promise<boolean> {
    listeners = [];
    return await Beacon.initialize();
  },
  watchBeacons(callback: (beacons: Beacon[]) => void) {
    if (Platform.OS === 'ios') {
      const listener = myModuleEvt.addListener('watchBeacons', callback);

      return {
        remove() {
          listener.remove();
        },
      };
    }

    listeners.push(callback);

    return {
      remove() {
        // remove listener from listeners array
        const index = listeners.indexOf(callback);
        if (index > -1) {
          listeners.splice(index, 1);
        }
      },
    };
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
  startBeaconScan(beacons: BeaconPayload[]) {
    Beacon.startBeaconScan(beacons);
  },
  stopBeaconScan() {
    if (Platform.OS === 'ios') {
      myModuleEvt.removeAllListeners('watchBeacons');
    }
    Beacon.stopBeaconScan();
  },
};
