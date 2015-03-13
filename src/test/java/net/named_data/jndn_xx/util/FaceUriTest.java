/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015 Alexander Afanasyev.
 *
 * This file is part of jndn-xx library.
 *
 * jndn-xx library is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * jndn-xx library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received copies of the GNU General Public License and GNU Lesser
 * General Public License along with jndn-xx, e.g., in COPYING.md file.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * See AUTHORS.md for complete list of jndn-xx authors and contributors.
 */

package net.named_data.jndn_xx.util;

import org.junit.Test;

import java.net.Inet4Address;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FaceUriTest {

  @Test
  public void ParseInternal() {
    FaceUri uri = new FaceUri();

    assertTrue(uri.parse("internal://"));
    assertEquals("internal", uri.getScheme());
    assertEquals("", uri.getHost());
    assertEquals("", uri.getPort());
    assertEquals("", uri.getPath());

    assertEquals(uri.parse("internal:"), false);
    assertEquals(uri.parse("internal:/"), false);
  }

  @Test
  public void ParseUdp()
  {
    new FaceUri("udp://hostname:6363");

    try {
      new FaceUri("udp//hostname:6363");
      fail("FaceUri.Error exception is expected");
    }
    catch (FaceUri.Error e) {
    }

    try {
      new FaceUri("udp://hostname:port");
      fail("FaceUri.Error exception is expected");
    }
    catch (FaceUri.Error e) {
    }

    FaceUri uri = new FaceUri();
    assertEquals(uri.parse("udp//hostname:6363"), false);

    assertTrue(uri.parse("udp://hostname:80"));
    assertEquals(uri.getScheme(), "udp");
    assertEquals(uri.getHost(), "hostname");
    assertEquals(uri.getPort(), "80");
    assertEquals(uri.getPath(), "");

    assertTrue(uri.parse("udp4://192.0.2.1:20"));
    assertEquals(uri.getScheme(), "udp4");
    assertEquals(uri.getHost(), "192.0.2.1");
    assertEquals(uri.getPort(), "20");
    assertEquals(uri.getPath(), "");

    assertTrue(uri.parse("udp6://[2001:db8:3f9:0::1]:6363"));
    assertEquals(uri.getScheme(), "udp6");
    assertEquals(uri.getHost(), "2001:db8:3f9:0::1");
    assertEquals(uri.getPort(), "6363");
    assertEquals(uri.getPath(), "");

    assertTrue(uri.parse("udp6://[2001:db8:3f9:0:3025:ccc5:eeeb:86d3]:6363"));
    assertEquals(uri.getScheme(), "udp6");
    assertEquals(uri.getHost(), "2001:db8:3f9:0:3025:ccc5:eeeb:86d3");
    assertEquals(uri.getPort(), "6363");
    assertEquals(uri.getPath(), "");

    assertEquals(uri.parse("udp6://[2001:db8:3f9:0:3025:ccc5:eeeb:86dg]:6363"), false);
  }

  @Test
  public void CheckCanonicalUdp() {
   assertEquals(FaceUri.canCanonize("udp"), true);
   assertEquals(FaceUri.canCanonize("udp4"), true);
   assertEquals(FaceUri.canCanonize("udp6"), true);

   assertEquals(new FaceUri("udp4://192.0.2.1:6363").isCanonical(), true);
   assertEquals(new FaceUri("udp://192.0.2.1:6363").isCanonical(), false);
   assertEquals(new FaceUri("udp4://192.0.2.1").isCanonical(), false);
   assertEquals(new FaceUri("udp4://192.0.2.1:6363/").isCanonical(), false);
   assertEquals(new FaceUri("udp6://[2001:db8::1]:6363").isCanonical(), true);
   assertEquals(new FaceUri("udp6://[2001:db8::01]:6363").isCanonical(), false);
   assertEquals(new FaceUri("udp://example.net:6363").isCanonical(), false);
   assertEquals(new FaceUri("udp4://example.net:6363").isCanonical(), false);
   assertEquals(new FaceUri("udp6://example.net:6363").isCanonical(), false);
   assertEquals(new FaceUri("udp4://224.0.23.170:56363").isCanonical(), true);
  }

  private static void
  addTest(String testUri, boolean shouldSucceed, String canonicalUri) throws FaceUri.CanonizeError
  {
    FaceUri uri = new FaceUri(testUri);
    if (shouldSucceed) {
      assertEquals(canonicalUri, uri.canonize().toString());
    }
    else {
      try {
        uri.canonize();
        fail("Canonization should have failed");
      }
      catch (FaceUri.CanonizeError e) {
      }
    }
  }

  @Test
  public void CanonizeUdpV4() throws FaceUri.CanonizeError
  {
    // IPv4 unicast
    addTest("udp4://192.0.2.1:6363", true, "udp4://192.0.2.1:6363");
    addTest("udp://192.0.2.2:6363", true, "udp4://192.0.2.2:6363");
    addTest("udp4://192.0.2.3", true, "udp4://192.0.2.3:6363");
    addTest("udp4://192.0.2.4:6363/", true, "udp4://192.0.2.4:6363");
    addTest("udp4://192.0.2.5:9695", true, "udp4://192.0.2.5:9695");
    addTest("udp4://192.0.2.666:6363", false, "");
    addTest("udp4://google-public-dns-a.google.com", true, "udp4://8.8.8.8:6363");
    addTest("udp4://invalid.invalid", false, "");

    // IPv4 multicast
    addTest("udp4://224.0.23.170:56363", true, "udp4://224.0.23.170:56363");
    addTest("udp4://224.0.23.170", true, "udp4://224.0.23.170:56363");
    addTest("udp4://all-routers.mcast.net:56363", true, "udp4://224.0.0.2:56363");
  }

  @Test
  public void CanonizeUdpV6() throws FaceUri.CanonizeError
  {
    // IPv6 unicast
    addTest("udp6://[2001:db8::1]:6363", true, "udp6://[2001:db8::1]:6363");
    addTest("udp://[2001:db8::1]:6363", true, "udp6://[2001:db8::1]:6363");
    addTest("udp6://[2001:db8::01]:6363", true, "udp6://[2001:db8::1]:6363");
    addTest("udp6://google-public-dns-a.google.com", true, "udp6://[2001:4860:4860::8888]:6363");
    addTest("udp6://invalid.invalid", false, "");
    addTest("udp://invalid.invalid", false, "");

    // IPv6 multicast
    addTest("udp6://[ff02::2]:56363", true, "udp6://[ff02::2]:56363");
    addTest("udp6://[ff02::2]", true, "udp6://[ff02::2]:56363");
  }

  @Test
  public void ParseTcp()
  {
    FaceUri uri = new FaceUri();

    assertTrue(uri.parse("tcp://random.host.name"));
    assertEquals(uri.getScheme(), "tcp");
    assertEquals(uri.getHost(), "random.host.name");
    assertEquals(uri.getPort(), "");
    assertEquals(uri.getPath(), "");

    assertEquals(uri.parse("tcp://192.0.2.1:"), false);
    assertEquals(uri.parse("tcp://[::zzzz]"), false);
  }

  @Test
  public void CheckCanonicalTcp()
  {
    assertEquals(FaceUri.canCanonize("tcp"), true);
    assertEquals(FaceUri.canCanonize("tcp4"), true);
    assertEquals(FaceUri.canCanonize("tcp6"), true);

    assertEquals(new FaceUri("tcp4://192.0.2.1:6363").isCanonical(), true);
    assertEquals(new FaceUri("tcp://192.0.2.1:6363").isCanonical(), false);
    assertEquals(new FaceUri("tcp4://192.0.2.1").isCanonical(), false);
    assertEquals(new FaceUri("tcp4://192.0.2.1:6363/").isCanonical(), false);
    assertEquals(new FaceUri("tcp6://[2001:db8::1]:6363").isCanonical(), true);
    assertEquals(new FaceUri("tcp6://[2001:db8::01]:6363").isCanonical(), false);
    assertEquals(new FaceUri("tcp://example.net:6363").isCanonical(), false);
    assertEquals(new FaceUri("tcp4://example.net:6363").isCanonical(), false);
    assertEquals(new FaceUri("tcp6://example.net:6363").isCanonical(), false);
    assertEquals(new FaceUri("tcp4://224.0.23.170:56363").isCanonical(), false);
  }

  @Test
  public void CanonizeTcpV4() throws FaceUri.CanonizeError
  {
    // IPv4 unicast
    addTest("tcp4://192.0.2.1:6363", true, "tcp4://192.0.2.1:6363");
    addTest("tcp://192.0.2.2:6363", true, "tcp4://192.0.2.2:6363");
    addTest("tcp4://192.0.2.3", true, "tcp4://192.0.2.3:6363");
    addTest("tcp4://192.0.2.4:6363/", true, "tcp4://192.0.2.4:6363");
    addTest("tcp4://192.0.2.5:9695", true, "tcp4://192.0.2.5:9695");
    addTest("tcp4://192.0.2.666:6363", false, "");
    addTest("tcp4://google-public-dns-a.google.com", true, "tcp4://8.8.8.8:6363");
    addTest("tcp4://invalid.invalid", false, "");

    // IPv4 multicast
    addTest("tcp4://224.0.23.170:56363", false, "");
    addTest("tcp4://224.0.23.170", false, "");
    addTest("tcp4://all-routers.mcast.net:56363", false, "");
  }

  @Test
  public void CanonizeTcpV6() throws FaceUri.CanonizeError
  {
    // IPv6 unicast
    addTest("tcp6://[2001:db8::1]:6363", true, "tcp6://[2001:db8::1]:6363");
    addTest("tcp://[2001:db8::1]:6363", true, "tcp6://[2001:db8::1]:6363");
    addTest("tcp6://[2001:db8::01]:6363", true, "tcp6://[2001:db8::1]:6363");
    addTest("tcp6://google-public-dns-a.google.com", true, "tcp6://[2001:4860:4860::8888]:6363");
    addTest("tcp6://invalid.invalid", false, "");
    addTest("tcp://invalid.invalid", false, "");

    // IPv6 multicast
    addTest("tcp6://[ff02::2]:56363", false, "");
    addTest("tcp6://[ff02::2]", false, "");
  }

  @Test
  public void ParseUnix()
  {
    FaceUri uri = new FaceUri();

    assertTrue(uri.parse("unix:///var/run/example.sock"));
    assertEquals(uri.getScheme(), "unix");
    assertEquals(uri.getHost(), "");
    assertEquals(uri.getPort(), "");
    assertEquals(uri.getPath(), "/var/run/example.sock");

    //assertEquals(uri.parse("unix://var/run/example.sock"), false);
    // This is not a valid unix:/ URI, but the parse would treat "var" as host
  }

  @Test
  public void ParseFd()
  {
    FaceUri uri = new FaceUri();

    assertTrue(uri.parse("fd://6"));
    assertEquals(uri.getScheme(), "fd");
    assertEquals(uri.getHost(), "6");
    assertEquals(uri.getPort(), "");
    assertEquals(uri.getPath(), "");
  }

  @Test
  public void ParseEther()
  {
    FaceUri uri = new FaceUri();

    assertTrue(uri.parse("ether://[08:00:27:01:dd:01]"));
    assertEquals(uri.getScheme(), "ether");
    assertEquals(uri.getHost(), "08:00:27:01:dd:01");
    assertEquals(uri.getPort(), "");
    assertEquals(uri.getPath(), "");

    assertEquals(uri.parse("ether://[08:00:27:zz:dd:01]"), false);
  }

  // @Test
  // public void CanonizeEther()
  // {
  //   // not supported (yet?)
  //   assertEquals(FaceUri.canCanonize("ether"), true);

  //   assertEquals(FaceUri("ether://[08:00:27:01:01:01]").isCanonical(), true);
  //   assertEquals(FaceUri("ether://[08:00:27:1:1:1]").isCanonical(), false);
  //   assertEquals(FaceUri("ether://[08:00:27:01:01:01]/").isCanonical(), false);
  //   assertEquals(FaceUri("ether://[33:33:01:01:01:01]").isCanonical(), true);

  //   addTest("ether://[08:00:27:01:01:01]", true, "ether://[08:00:27:01:01:01]");
  //   addTest("ether://[08:00:27:1:1:1]", true, "ether://[08:00:27:01:01:01]");
  //   addTest("ether://[08:00:27:01:01:01]/", true, "ether://[08:00:27:01:01:01]");
  //   addTest("ether://[33:33:01:01:01:01]", true, "ether://[33:33:01:01:01:01]");
  // }

  @Test
  public void ParseDev()
  {
    FaceUri uri = new FaceUri();

    assertTrue(uri.parse("dev://eth0"));
    assertEquals(uri.getScheme(), "dev");
    assertEquals(uri.getHost(), "eth0");
    assertEquals(uri.getPort(), "");
    assertEquals(uri.getPath(), "");
  }

  @Test
  public void CanonizeUnsupported() throws FaceUri.CanonizeError
  {
    assertEquals(FaceUri.canCanonize("internal"), false);
    assertEquals(FaceUri.canCanonize("null"), false);
    assertEquals(FaceUri.canCanonize("unix"), false);
    assertEquals(FaceUri.canCanonize("fd"), false);
    assertEquals(FaceUri.canCanonize("dev"), false);

    assertEquals(new FaceUri("internal://").isCanonical(), false);
    assertEquals(new FaceUri("null://").isCanonical(), false);
    assertEquals(new FaceUri("unix:///var/run/nfd.sock").isCanonical(), false);
    assertEquals(new FaceUri("fd://0").isCanonical(), false);
    assertEquals(new FaceUri("dev://eth1").isCanonical(), false);

    addTest("internal://", false, "");
    addTest("null://", false, "");
    addTest("unix:///var/run/nfd.sock", false, "");
    addTest("fd://0", false, "");
    addTest("dev://eth1", false, "");
  }

  @Test
  public void Bug1635()
  {
    FaceUri uri = new FaceUri();

    assertTrue(uri.parse("wsclient://[::ffff:76.90.11.239]:56366"));
    assertEquals(uri.getScheme(), "wsclient");
    assertEquals(uri.getHost(), "76.90.11.239");
    assertEquals(uri.getPort(), "56366");
    assertEquals(uri.getPath(), "");
    assertEquals(uri.toString(), "wsclient://76.90.11.239:56366");
  }

}
