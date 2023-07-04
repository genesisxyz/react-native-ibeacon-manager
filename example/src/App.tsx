import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import Beacon from 'react-native-beacon';

Beacon.setOptions({
  android: {
    notification: {
      contentTitle: 'Beacon scan',
      contentText: 'Beacon scan started',
    },
  },
});

export default function App() {
  const [scanStarted, setScanStarted] = React.useState(false);

  React.useEffect(() => {
    Beacon.init().then(async () => {
      await Beacon.requestPermissions();
    });
  }, []);

  React.useEffect(() => {
    const listener = Beacon.watchBeacons((beacons) => {
      console.log(beacons);
    });

    return () => {
      listener.remove();
    };
  }, []);

  const toggleScan = () => {
    if (scanStarted) {
      Beacon.stopBeaconScan();
      setScanStarted(false);
    } else {
      Beacon.startBeaconScan([
        {
          id: 'my-beacon',
          uuid: 'D9FF627C-FE7D-425C-9CE8-A0652E6738FF',
          major: 0,
          minor: 0,
        },
      ]);
      setScanStarted(true);
    }
  };

  return (
    <View style={styles.container}>
      <Text>Hello, World!</Text>
      <Button title="Enable BT" onPress={Beacon.enableBluetooth} />
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
