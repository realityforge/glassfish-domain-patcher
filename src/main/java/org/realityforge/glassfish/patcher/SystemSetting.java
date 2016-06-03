package org.realityforge.glassfish.patcher;

public class SystemSetting
{
  private final String _key;
  private final String _value;
  private boolean _updated;

  public SystemSetting( final String key, final String value )
  {
    _key = key;
    _value = value;
  }

  public String getKey()
  {
    return _key;
  }

  public String getValue()
  {
    return _value;
  }

  public boolean isUpdated()
  {
    return _updated;
  }

  public void setUpdated( final boolean updated )
  {
    _updated = updated;
  }
}
