package edu.northwestern.cbits.purple_robot_manager.calibration;

public class ContactRecord implements Comparable<ContactRecord>
{
    public int count = 1;
    public String number = null;
    public String name = null;
    public String group = null;

    public int compareTo(ContactRecord other)
    {
        if (this.count > other.count)
            return -1;
        else if (this.count < other.count)
            return 1;

        if (this.name != null)
            return this.name.compareTo(other.name);

        return this.number.compareTo(other.number);
    }
}
