#ifndef __TinyGPSPlus_h
#define __TinyGPSPlus_h

#include <sstream>
#include <string>

std::string doubleToString(double d)
{
    std::ostringstream ss;
    ss << d;
    return ss.str();
}

#include <limits.h>
#include <jni.h>

#define _GPS_VERSION "0.95" // software version of this library
#define _GPS_MPH_PER_KNOT 1.15077945
#define _GPS_MPS_PER_KNOT 0.51444444
#define _GPS_KMPH_PER_KNOT 1.852
#define _GPS_MILES_PER_METER 0.00062137112
#define _GPS_KM_PER_METER 0.001
#define _GPS_FEET_PER_METER 3.2808399
#define _GPS_MAX_FIELD_SIZE 15

struct RawDegrees
{
    int deg;
    long billionths;
    bool negative;
public:
    RawDegrees() : deg(0), billionths(0), negative(false)
    {}
};

struct TinyGPSLocation
{
    friend class TinyGPSPlus;
public:
    bool isValid() const    { return valid; }
    bool isUpdated() const  { return updated; }
    const RawDegrees &rawLat()     { updated = false; return rawLatData; }
    const RawDegrees &rawLng()     { updated = false; return rawLngData; }
    double lat();
    double lng();

    TinyGPSLocation() : valid(false), updated(false)
    {}

private:
    bool valid, updated;
    RawDegrees rawLatData, rawLngData, rawNewLatData, rawNewLngData;
    long lastCommitTime;
    void commit();
    void setLatitude(const char *term);
    void setLongitude(const char *term);
};

struct TinyGPSDate
{
    friend class TinyGPSPlus;
public:
    bool isValid() const       { return valid; }
    bool isUpdated() const     { return updated; }

    long value()           { updated = false; return date; }
    int year();
    int month();
    int day();

    TinyGPSDate() : valid(false), updated(false), date(0)
    {}

private:
    bool valid, updated;
    long date, newDate;
    long lastCommitTime;
    void commit();
    void setDate(const char *term);
};

struct TinyGPSTime
{
    friend class TinyGPSPlus;
public:
    bool isValid() const       { return valid; }
    bool isUpdated() const     { return updated; }

    long value()           { updated = false; return time; }
    int hour();
    int minute();
    int second();
    int centisecond();

    TinyGPSTime() : valid(false), updated(false), time(0)
    {}

private:
    bool valid, updated;
    long time, newTime;
    long lastCommitTime;
    void commit();
    void setTime(const char *term);
};

struct TinyGPSDecimal
{
    friend class TinyGPSPlus;
public:
    bool isValid() const    { return valid; }
    bool isUpdated() const  { return updated; }
    long value()         { updated = false; return val; }

    TinyGPSDecimal() : valid(false), updated(false), val(0)
    {}

private:
    bool valid, updated;
    long lastCommitTime;
    long val, newval;
    void commit();
    void set(const char *term);
};

struct TinyGPSInteger
{
    friend class TinyGPSPlus;
public:
    bool isValid() const    { return valid; }
    bool isUpdated() const  { return updated; }
    long value()        { updated = false; return val; }

    TinyGPSInteger() : valid(false), updated(false), val(0)
    {}

private:
    bool valid, updated;
    long lastCommitTime;
    long val, newval;
    void commit();
    void set(const char *term);
};

struct TinyGPSSpeed : TinyGPSDecimal
{
    double knots()    { return value() / 100.0; }
    double mph()      { return _GPS_MPH_PER_KNOT * value() / 100.0; }
    double mps()      { return _GPS_MPS_PER_KNOT * value() / 100.0; }
    double kmph()     { return _GPS_KMPH_PER_KNOT * value() / 100.0; }
};

struct TinyGPSCourse : public TinyGPSDecimal
{
    double deg()      { return value() / 100.0; }
};

struct TinyGPSAltitude : TinyGPSDecimal
{
    double meters()       { return value() / 100.0; }
    double miles()        { return _GPS_MILES_PER_METER * value() / 100.0; }
    double kilometers()   { return _GPS_KM_PER_METER * value() / 100.0; }
    double feet()         { return _GPS_FEET_PER_METER * value() / 100.0; }
};

class TinyGPSPlus;
class TinyGPSCustom
{
public:
    TinyGPSCustom() {};
    TinyGPSCustom(TinyGPSPlus &gps, const char *sentenceName, int termNumber);
    void begin(TinyGPSPlus &gps, const char *_sentenceName, int _termNumber);

    bool isUpdated() const  { return updated; }
    bool isValid() const    { return valid; }
    const char *value()     { updated = false; return buffer; }

private:
    void commit();
    void set(const char *term);

    char stagingBuffer[_GPS_MAX_FIELD_SIZE + 1];
    char buffer[_GPS_MAX_FIELD_SIZE + 1];
    unsigned long lastCommitTime;
    bool valid, updated;
    const char *sentenceName;
    int termNumber;
    friend class TinyGPSPlus;
    TinyGPSCustom *next;
};

class TinyGPSPlus
{
public:
    TinyGPSPlus();
    bool encode(char c); // process one character received from GPS
    TinyGPSPlus &operator << (char c) {encode(c); return *this;}

    TinyGPSLocation location;
    TinyGPSDate date;
    TinyGPSTime time;
    TinyGPSSpeed speed;
    TinyGPSCourse course;
    TinyGPSAltitude altitude;
    TinyGPSInteger satellites;
    TinyGPSDecimal hdop;

    static const char *libraryVersion() { return _GPS_VERSION; }

    static double distanceBetween(double lat1, double long1, double lat2, double long2);
    static double courseTo(double lat1, double long1, double lat2, double long2);
    static const char *cardinal(double course);

    static long parseDecimal(const char *term);
    static void parseDegrees(const char *term, RawDegrees &deg);

    long charsProcessed()   const { return encodedCharCount; }
    long sentencesWithFix() const { return sentencesWithFixCount; }
    long failedChecksum()   const { return failedChecksumCount; }
    long passedChecksum()   const { return passedChecksumCount; }

private:
    enum {GPS_SENTENCE_GPGGA, GPS_SENTENCE_GPRMC, GPS_SENTENCE_OTHER};

    // parsing state variables
    int parity;
    bool isChecksumTerm;
    char term[_GPS_MAX_FIELD_SIZE];
    int curSentenceType;
    int curTermNumber;
    int curTermOffset;
    bool sentenceHasFix;

    // custom element support
    friend class TinyGPSCustom;
    TinyGPSCustom *customElts;
    TinyGPSCustom *customCandidates;
    void insertCustom(TinyGPSCustom *pElt, const char *sentenceName, int index);

    // statistics
    long encodedCharCount;
    long sentencesWithFixCount;
    long failedChecksumCount;
    long passedChecksumCount;

    // internal utilities
    int fromHex(char a);
    bool endOfTermHandler();
};

TinyGPSPlus tinyGPSPlus;

extern "C" {
JNIEXPORT void JNICALL
Java_com_maiaga_Processor_encode(JNIEnv *env, jobject stuff, jshort s) {
    tinyGPSPlus.encode((char)s);
}

JNIEXPORT jdouble JNICALL
Java_com_maiaga_Processor_lat(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.location.lat();
}

JNIEXPORT jdouble JNICALL
Java_com_maiaga_Processor_lng(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.location.lng();
}

JNIEXPORT jdouble JNICALL
Java_com_maiaga_Processor_alt(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.altitude.meters();
}

JNIEXPORT jdouble JNICALL
Java_com_maiaga_Processor_spd(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.speed.mps();
}

JNIEXPORT jlong JNICALL
Java_com_maiaga_Processor_date(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.date.value();
}

JNIEXPORT jlong JNICALL
Java_com_maiaga_Processor_time(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.time.value();
}

JNIEXPORT jboolean JNICALL
Java_com_maiaga_Processor_isValidLoc(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.location.isValid();
}

JNIEXPORT jboolean JNICALL
Java_com_maiaga_Processor_isValidSpd(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.speed.isValid();
}

JNIEXPORT jboolean JNICALL
Java_com_maiaga_Processor_isValidAlt(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.altitude.isValid();
}

JNIEXPORT jboolean JNICALL
Java_com_maiaga_Processor_isValidDateTime(JNIEnv *env, jobject instance) {
    return tinyGPSPlus.date.isValid() && tinyGPSPlus.time.isValid();
}

JNIEXPORT jboolean JNICALL
Java_com_maiaga_Processor_newDataAvailable(JNIEnv *env, jobject instance) {
    return (
            tinyGPSPlus.altitude.isUpdated() ||
            tinyGPSPlus.speed.isUpdated() ||
            tinyGPSPlus.location.isUpdated()
    );
}
}
#endif // def(__TinyGPSPlus_h)
