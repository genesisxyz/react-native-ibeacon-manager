import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import Beacon from 'react-native-beacon';

export default function App() {
  const [scanStarted, setScanStarted] = React.useState(false);

  const toggleScan = () => {
    if (scanStarted) {
      Beacon.stopBeaconScan();
      setScanStarted(false);
    } else {
      Beacon.requestPermissions().then(async () => {
        await Beacon.init({
          registerBeaconsTask: (beacons) => {
            console.log('beacons', beacons);
          },
        });
        Beacon.startBeaconScan([
          {
            id: 'my-beacon',
            uuid: 'D9FF627C-FE7D-425C-9CE8-A0652E6738FF',
            major: 0,
            minor: 0,
          },
          {
            id: 'my-beacon-2',
            uuid: '99504E1B-4DD7-4DAC-984C-44318E7E32E7',
            major: 0,
            minor: 0,
          },
          {
            id: 'my-beacon-3',
            uuid: '1D4A42A9-01C2-42BF-A648-C6E15DF32283',
            major: 0,
            minor: 0,
          },
        ]);
        setScanStarted(true);
      });
    }
  };

  return (
    <View style={styles.container}>
      <Text>Hello, World!</Text>
      <Button
        title={scanStarted ? 'Stop scan' : 'Start scan'}
        onPress={toggleScan}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
