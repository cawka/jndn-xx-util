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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.net.InetAddresses;

/**
 * Represents the underlying protocol and address used by Face
 */
public class FaceUri implements Cloneable {
  public static class Error extends IllegalArgumentException {
    Error(String error) {
      super(error);
    }
  }

  public static class CanonizeError extends Exception {
    CanonizeError(String error) {
      super(error);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  public FaceUri()
  {
  }

  /**
   * Construct FaceUri from string
   * @param uri scheme://host[:port]/path
   * @throws Error if URI cannot be parsed
   */
  public FaceUri(String uri) throws Error {
    if (!parse(uri)) {
      throw new Error("Malformed URI: " + uri);
    }
  }

  /**
   * Exception-safe parsing
   * @param uri FaceUri to parse
   * @return true if <pre>uri</pre> is successfully parsed
   */
  public boolean
  parse(String uri) {
    m_scheme = "";
    m_host = "";
    m_isV6 = false;
    m_port = "";
    m_path = "";

    Pattern protocolExp = Pattern.compile("(\\w+\\d?)://([^/]*)(\\/[^?]*)?");
    Matcher protocolMatch = protocolExp.matcher(uri);
    if (!protocolMatch.matches()) {
      return false;
    }
    m_scheme = protocolMatch.group(1);
    if (m_scheme == null)
      m_scheme = "";
    String authority = protocolMatch.group(2);
    if (authority == null)
      authority = "";
    m_path = protocolMatch.group(3);
    if (m_path == null)
      m_path = "";

    // pattern for IPv6 address enclosed in [ ], with optional port number
    final Pattern v6Exp = Pattern.compile("^\\[([a-fA-F0-9:]+)\\](?:\\:(\\d+))?$");
    // pattern for Ethernet address in standard hex-digits-and-colons notation
    final Pattern etherExp = Pattern.compile("^\\[((?:[a-fA-F0-9]{1,2}\\:){5}(?:[a-fA-F0-9]{1,2}))\\]$");
    // pattern for IPv4-mapped IPv6 address, with optional port number
    final Pattern v4MappedV6Exp = Pattern.compile("^\\[::ffff:(\\d+(?:\\.\\d+){3})\\](?:\\:(\\d+))?$");
    // pattern for IPv4/hostname/fd/ifname, with optional port number
    final Pattern v4HostExp = Pattern.compile("^([^:]+)(?:\\:(\\d+))?$");

    if (authority.equals("")) {
      // UNIX, internal
    } else {
      Matcher match = v6Exp.matcher(authority);
      m_isV6 = match.matches();
      if (m_isV6 ||
        (match = etherExp.matcher(authority)).matches() ||
        (match = v4MappedV6Exp.matcher(authority)).matches() ||
        (match = v4HostExp.matcher(authority)).matches()) {
        m_host = match.group(1);
        if (m_host == null)
          m_host = "";
        m_port = match.group(2);
        if (m_port == null)
          m_port = "";
      } else {
        return false;
      }
    }

    return true;
  }

  //////////////////////////////////////////////////////////////////////////////
  // getters

  /**
   * Get scheme (protocol)
   * @return scheme (return "" when empty)
   */
  public String
  getScheme() {
    return m_scheme;
  }

  /**
   * Get host (domain)
   * @return host (return "" when empty)
   */
  public String
  getHost() {
    return m_host;
  }

  /**
   * Get port
   * @return port number (return "" when empty)
   */
  public String
  getPort() {
    return m_port;
  }

  /**
   * Get path
   * @return path (return "" when empty)
   */
  public String
  getPath() {
    return m_path;
  }

  /**
   * Convert FaceUri instance to a string
   * @return string represenation of FaceUri
   */
  public String
  toString() {
    String out;
    out = m_scheme + "://";
    if (m_isV6) {
      out += "[" + m_host + "]";
    } else {
      out += m_host;
    }
    if (!m_port.equals("")) {
      out += ":" + m_port;
    }
    out += m_path;
    return out;
  }

  //////////////////////////////////////////////////////////////////////////////
  // comparator

  /**
   * Compare FaceUris
   * @param rhs FaceUri to compare with
   * @return true if <pre>this</pre> is equal to <pre>rhs</pre>
   */
  boolean
  equals(FaceUri rhs) {
    return (m_scheme == rhs.m_scheme &&
      m_host == rhs.m_host &&
      m_isV6 == rhs.m_isV6 &&
      m_port == rhs.m_port &&
      m_path == rhs.m_path);
  }

  //////////////////////////////////////////////////////////////////////////////
  // canonical FaceUri

  /**
   * Check whether FaceUri of the scheme can be canonized
   * @param scheme scheme to check
   * @return true if FaceUri supports canonization for the scheme
   */
  static public boolean
  canCanonize(String scheme) {
    return s_canonizeProviders.containsKey(scheme);
  }

  /**
   * Determine whether this FaceUri is in canonical form
   * <p>
   * Note that this method can block for DNS resolution process
   * <p>
   * @return true if this FaceUri is in canonical form,
   * false if this FaceUri is not in canonical form or
   * or it's undetermined whether this FaceUri is in canonical form
   */
  public boolean
  isCanonical() {
    CanonizeProvider provider = s_canonizeProviders.get(m_scheme);
    if (provider == null)
      return false;

    return provider.isCanonical(this);
  }

  /**
   * Convert this FaceUri to canonical form
   * <p>
   * Note that this method can block for DNS resolution process
   * <p>
   * @return A new FaceUri in canonical form; this FaceUri is unchanged
   * @throws CanonizeError when canonization fails
   */
  public FaceUri
  canonize() throws CanonizeError {
    CanonizeProvider provider = s_canonizeProviders.get(m_scheme);
    if (provider == null) {
      throw new CanonizeError(this.toString() + " does not support canonization");
    }

    return provider.canonize(this);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Interface that provides FaceUri canonization functionality for a group of schemes
   */
  private interface CanonizeProvider {
    public Set<String>
    getSchemes();

    public boolean
    isCanonical(FaceUri faceUri);

    public FaceUri
    canonize(FaceUri faceUri) throws CanonizeError;
  }

  /**
   * Canonizer for IPv4 and IPv6-based schemes
   */
  private static class IpHostCanonizeProvider implements CanonizeProvider {
    public Set<String>
    getSchemes() {
      Set<String> schemes = new HashSet<String>();
      schemes.add(m_baseScheme);
      schemes.add(m_v4Scheme);
      schemes.add(m_v6Scheme);
      return schemes;
    }

    public boolean
    isCanonical(FaceUri faceUri) {
      if (faceUri.getPort().equals("")) {
        return false;
      }
      if (!faceUri.getPath().equals("")) {
        return false;
      }

      try {
        InetAddress addr;
        if (faceUri.getScheme().equals(m_v4Scheme)) {
          addr = Inet4Address.getByName(faceUri.getHost());
        } else if (faceUri.getScheme().equals(m_v6Scheme)) {
          addr = Inet6Address.getByName(faceUri.getHost());
        } else {
          return false;
        }
        return InetAddresses.toAddrString(addr).equals(faceUri.getHost()) && this.checkAddress(addr);
      } catch (UnknownHostException e) {
        return false;
      }
    }

    public FaceUri
    canonize(FaceUri faceUri) throws CanonizeError {

      if (this.isCanonical(faceUri)) {
        try {
          return (FaceUri)faceUri.clone();
        }
        catch (CloneNotSupportedException e) {
          assert false;
          return null;
        }
      }

      InetAddress addr = null;
      try {
        if (faceUri.getScheme().equals(m_v4Scheme)) {
          for (InetAddress a : InetAddress.getAllByName(faceUri.getHost())) {
            if (a instanceof Inet4Address) {
              addr = a;
              break;
            }
          }
        } else if (faceUri.getScheme().equals(m_v6Scheme)) {
          for (InetAddress a : InetAddress.getAllByName(faceUri.getHost())) {
            if (a instanceof Inet6Address) {
              addr = a;
              break;
            }
          }
        } else {
          addr = InetAddress.getByName(faceUri.getHost());
        }
      } catch (UnknownHostException e) {
        throw new CanonizeError("Cannot resolve " + faceUri.getHost());
      }

      if (addr == null) {
        throw new CanonizeError("Could not resolve " + faceUri.getHost() + " for scheme " + faceUri.getScheme());
      }

      if (!this.checkAddress(addr)) {
        throw new CanonizeError("Resolved to " + addr.getHostAddress() + ", which is prohibied by the CanonizeProvider");
      }

      int port = 0;
      if (faceUri.getPort().equals("")) {
        port = addr.isMulticastAddress() ? m_defaultMulticastPort : m_defaultUnicastPort;
      } else {
        try {
          port = Integer.valueOf(faceUri.getPort());
        } catch (NumberFormatException e) {
          throw new CanonizeError("Invalid port number");
        }
      }

      faceUri = new FaceUri();
      if (addr instanceof Inet4Address) {
        faceUri.parse(m_v4Scheme + "://" + InetAddresses.toAddrString(addr) + ":" + String.valueOf(port));
      } else if (addr instanceof Inet6Address) {
        faceUri.parse(m_v6Scheme + "://[" + InetAddresses.toAddrString(addr) + "]:" + String.valueOf(port));
      } else {
        throw new CanonizeError("Unknown type of address: " + addr.getHostAddress());
      }

      return faceUri;
    }

    //////////////////////////////////////////////////////////////////////////////

    protected IpHostCanonizeProvider(String baseScheme, int defaultUnicastPort, int defaultMulticastPort) {
      m_baseScheme = baseScheme;
      m_v4Scheme = baseScheme + "4";
      m_v6Scheme = baseScheme + "6";
      m_defaultUnicastPort = defaultUnicastPort;
      m_defaultMulticastPort = defaultMulticastPort;
    }

    protected IpHostCanonizeProvider(String baseScheme) {
      this(baseScheme, 6363, 56363);
    }

    /**
     * @return (true, ignored) if the address is allowable;
     * (false,reason) if the address is not allowable.
     * @brief when overriden in a subclass, check the IP address is allowable
     */
    protected boolean
    checkAddress(InetAddress ipAddress) {
      return true;
    }

    //////////////////////////////////////////////////////////////////////////////

    private String m_baseScheme = "";
    private String m_v4Scheme = "";
    private String m_v6Scheme = "";
    private int m_defaultUnicastPort = 6363;
    private int m_defaultMulticastPort = 56363;
  }

  private static class UdpCanonizeProvider extends IpHostCanonizeProvider {
    public UdpCanonizeProvider() {
      super("udp");
    }
  }

  private static class TcpCanonizeProvider extends IpHostCanonizeProvider {
    public TcpCanonizeProvider() {
      super("tcp");
    }

    protected boolean
    checkAddress(InetAddress ipAddress) {
      return !ipAddress.isMulticastAddress();
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  static private Map<String, CanonizeProvider>
  initCanonizeProviders()
  {
    Map<String, CanonizeProvider> providers = new HashMap<String, CanonizeProvider>();
    addCanonizeProvider(providers, new TcpCanonizeProvider());
    addCanonizeProvider(providers, new UdpCanonizeProvider());
    return providers;
  }

  private static void
  addCanonizeProvider(Map<String, CanonizeProvider> providers, CanonizeProvider provider) {
    Set<String> schemes = provider.getSchemes();
    assert !schemes.isEmpty();

    for (String scheme : schemes) {
      assert !providers.containsKey(scheme);
      providers.put(scheme, provider);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  private String m_scheme = "";
  private String m_host = "";
  boolean m_isV6 = false; ///< whether to add [] around host when writing string
  private String m_port = "";
  private String m_path = "";

  static private final Map<String, CanonizeProvider> s_canonizeProviders = initCanonizeProviders();
}
