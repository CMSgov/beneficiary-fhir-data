CCW Sample Data Generation
==========================

## Performance


### Data From `SAMPLE_D`

As of commit `6c99035`, the following metric output was observed from running `SampleDataLoaderTest.noErrorsFromSampleTestD()`:

```
SampleDataLoader - Committed DE-SynPUF sample 'SAMPLE_TEST_D'.
SampleDataLoaderTest - type=GAUGE, name=PS-MarkSweep.count, value=11
SampleDataLoaderTest - type=GAUGE, name=PS-MarkSweep.time, value=65439
SampleDataLoaderTest - type=GAUGE, name=PS-Scavenge.count, value=60
SampleDataLoaderTest - type=GAUGE, name=PS-Scavenge.time, value=34450
SampleDataLoaderTest - type=GAUGE, name=heap.committed, value=16018046976
SampleDataLoaderTest - type=GAUGE, name=heap.init, value=526385152
SampleDataLoaderTest - type=GAUGE, name=heap.max, value=30542397440
SampleDataLoaderTest - type=GAUGE, name=heap.usage, value=0.2355609881029693
SampleDataLoaderTest - type=GAUGE, name=heap.used, value=7194597320
SampleDataLoaderTest - type=GAUGE, name=non-heap.committed, value=59047936
SampleDataLoaderTest - type=GAUGE, name=non-heap.init, value=2555904
SampleDataLoaderTest - type=GAUGE, name=non-heap.max, value=-1
SampleDataLoaderTest - type=GAUGE, name=non-heap.usage, value=-5.7207688E7
SampleDataLoaderTest - type=GAUGE, name=non-heap.used, value=57207752
SampleDataLoaderTest - type=GAUGE, name=pools.Code-Cache.committed, value=19988480
SampleDataLoaderTest - type=GAUGE, name=pools.Code-Cache.init, value=2555904
SampleDataLoaderTest - type=GAUGE, name=pools.Code-Cache.max, value=251658240
SampleDataLoaderTest - type=GAUGE, name=pools.Code-Cache.usage, value=0.07716395060221354
SampleDataLoaderTest - type=GAUGE, name=pools.Code-Cache.used, value=19418944
SampleDataLoaderTest - type=GAUGE, name=pools.Metaspace.committed, value=39059456
SampleDataLoaderTest - type=GAUGE, name=pools.Metaspace.init, value=0
SampleDataLoaderTest - type=GAUGE, name=pools.Metaspace.max, value=-1
SampleDataLoaderTest - type=GAUGE, name=pools.Metaspace.usage, value=0.9674778880688968
SampleDataLoaderTest - type=GAUGE, name=pools.Metaspace.used, value=37789160
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Eden-Space.committed, value=3818389504
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Eden-Space.init, value=132120576
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Eden-Space.max, value=3818389504
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Eden-Space.usage, value=0.24005261669606767
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Eden-Space.used, value=916614392
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Old-Gen.committed, value=8382316544
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Old-Gen.init, value=351272960
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Old-Gen.max, value=22906667008
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Old-Gen.usage, value=0.2691555962221285
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Old-Gen.used, value=6165457616
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Survivor-Space.committed, value=3817340928
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Survivor-Space.init, value=21495808
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Survivor-Space.max, value=3817340928
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Survivor-Space.usage, value=0.029477406949594834
SampleDataLoaderTest - type=GAUGE, name=pools.PS-Survivor-Space.used, value=112525312
SampleDataLoaderTest - type=GAUGE, name=total.committed, value=16077094912
SampleDataLoaderTest - type=GAUGE, name=total.init, value=528941056
SampleDataLoaderTest - type=GAUGE, name=total.max, value=30542397439
SampleDataLoaderTest - type=GAUGE, name=total.used, value=7251806096
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.beneficiaries.files, count=3, min=390.85459599999996, max=3044.5952469999997, mean=1328.0840253581189, stddev=1211.2414051458136, median=562.966642, p75=3044.5952469999997, p95=3044.5952469999997, p98=3044.5952469999997, p99=3044.5952469999997, p999=3044.5952469999997, mean_rate=0.003634138213563414, m1=6.96295087318122E-7, m5=0.03900135237782074, m15=0.24124633396533704, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.beneficiaries.records, count=29558, min=0.016246999999999998, max=1.3715899999999999, mean=0.09694584844324522, stddev=0.10748711839249102, median=0.052754999999999996, p75=0.15226599999999998, p95=0.22369599999999998, p98=0.4113, p99=0.5698219999999999, p999=1.099324, mean_rate=35.80573506343623, m1=0.006860363396983013, m5=384.26732452787513, m15=2376.9197131158094, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.carrier.files, count=2, min=0.731174, max=261598.27955099999, mean=130799.5053625, stddev=130798.7741885, median=261598.27955099999, p75=261598.27955099999, p95=261598.27955099999, p98=261598.27955099999, p99=261598.27955099999, p999=261598.27955099999, mean_rate=0.0024934425485301295, m1=4.289924690301605E-6, m5=0.0011112270911734425, m15=0.0012229754219809307, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.carrier.records, count=413674, min=0.268831, max=2.317187, mean=0.7370793739426057, stddev=0.15185284791092735, median=0.6982079999999999, p75=0.7781079999999999, p95=1.032177, p98=1.182137, p99=1.339559, p999=1.5802969999999998, mean_rate=515.7340248229384, m1=0.1665568145200173, m5=267.6367907013904, m15=937.0207333312928, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.drug.files, count=1, min=343527.794266, max=343527.794266, mean=343527.794266, stddev=0.0, median=343527.794266, p75=343527.794266, p95=343527.794266, p98=343527.794266, p99=343527.794266, p999=343527.794266, mean_rate=0.0018501097844325716, m1=6.200428968939233E-4, m5=0.0017257315456848036, m15=8.921842202525036E-4, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.drug.records, count=439981, min=0.487554, max=2.2296839999999998, mean=1.0014795931310292, stddev=0.13021464575594163, median=0.9903689999999999, p75=1.0049869999999999, p95=1.049709, p98=1.5491759999999999, p99=1.8225749999999998, p999=2.2296839999999998, mean_rate=814.0080512099393, m1=38.786338083555975, m5=694.5913164178332, m15=1217.142283207108, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.inpatient.files, count=1, min=2212.474652, max=2212.474652, mean=2212.474652, stddev=0.0, median=2212.474652, p75=2212.474652, p95=2212.474652, p98=2212.474652, p99=2212.474652, p999=2212.474652, mean_rate=0.0012172566041594815, m1=2.522686500226013E-7, m5=0.013218940663797796, m15=0.0808634404058075, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.inpatient.records, count=5804, min=0.224289, max=2.060588, mean=0.35180630465661494, stddev=0.14625759159308352, median=0.309682, p75=0.39835899999999996, p95=0.632079, p98=0.7424649999999999, p99=0.8690479999999999, p999=1.291444, mean_rate=7.064933238813061, m1=0.0014641672447311778, m5=76.72273161268244, m15=469.3314081153068, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.outpatient.files, count=1, min=17195.325036, max=17195.325036, mean=17195.325036, stddev=0.0, median=17195.325036, p75=17195.325036, p95=17195.325036, p98=17195.325036, p99=17195.325036, p999=17195.325036, mean_rate=0.0012205391696112964, m1=2.8149903968711966E-8, m5=2.3355236752561044E-4, m15=4.5806265200837813E-4, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.outpatient.records, count=69199, min=0.158574, max=1.437621, mean=0.19462243151957068, stddev=0.06322140832770516, median=0.172151, p75=0.181039, p95=0.306037, p98=0.369511, p99=0.41025, p999=0.525965, mean_rate=84.45984982656707, m1=0.005871086385240866, m5=233.55356087874844, m15=1363.8087070004417, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.samples, count=1, min=825488.776017, max=825488.776017, mean=825488.776017, stddev=0.0, median=825488.776017, p75=825488.776017, p95=825488.776017, p98=825488.776017, p99=825488.776017, p999=825488.776017, mean_rate=0.0012113478673243707, m1=0.0, m5=0.0, m15=0.0, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderTest - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.samples.tx, count=1, min=196952.50039499998, max=196952.50039499998, mean=196952.50039499998, stddev=0.0, median=196952.50039499998, p75=196952.50039499998, p95=196952.50039499998, p98=196952.50039499998, p99=196952.50039499998, p999=196952.50039499998, mean_rate=0.005076390372055678, m1=0.0, m5=0.0, m15=0.0, rate_unit=events/second, duration_unit=milliseconds
```

### Data From Disabling JDO in `SampleDataLoader` and running against `SAMPLE_1`

I was curious if it was possible to fit the full `SAMPLE_1` sample data set into memory. Turns out, it just barely is. If all of the `pm.makePersistent(...)` calls in `SampleDataLoader` are disabled, it turns out that the full generated data set *just* barely squeezes in to an 8 GB heap. Here's the data from that run:

```
SampleDataLoader - Committed DE-SynPUF sample 'SAMPLE_1'.
SampleDataLoaderIT - type=GAUGE, name=PS-MarkSweep.count, value=13
SampleDataLoaderIT - type=GAUGE, name=PS-MarkSweep.time, value=121356
SampleDataLoaderIT - type=GAUGE, name=PS-Scavenge.count, value=274
SampleDataLoaderIT - type=GAUGE, name=PS-Scavenge.time, value=55454
SampleDataLoaderIT - type=GAUGE, name=heap.committed, value=3792699392
SampleDataLoaderIT - type=GAUGE, name=heap.init, value=526385152
SampleDataLoaderIT - type=GAUGE, name=heap.max, value=7471628288
SampleDataLoaderIT - type=GAUGE, name=heap.usage, value=0.20098065135437315
SampleDataLoaderIT - type=GAUGE, name=heap.used, value=1501652720
SampleDataLoaderIT - type=GAUGE, name=non-heap.committed, value=59285504
SampleDataLoaderIT - type=GAUGE, name=non-heap.init, value=2555904
SampleDataLoaderIT - type=GAUGE, name=non-heap.max, value=-1
SampleDataLoaderIT - type=GAUGE, name=non-heap.usage, value=-5.638588E7
SampleDataLoaderIT - type=GAUGE, name=non-heap.used, value=56385880
SampleDataLoaderIT - type=GAUGE, name=pools.Code-Cache.committed, value=17956864
SampleDataLoaderIT - type=GAUGE, name=pools.Code-Cache.init, value=2555904
SampleDataLoaderIT - type=GAUGE, name=pools.Code-Cache.max, value=251658240
SampleDataLoaderIT - type=GAUGE, name=pools.Code-Cache.usage, value=0.06596094767252604
SampleDataLoaderIT - type=GAUGE, name=pools.Code-Cache.used, value=16599616
SampleDataLoaderIT - type=GAUGE, name=pools.Compressed-Class-Space.committed, value=4456448
SampleDataLoaderIT - type=GAUGE, name=pools.Compressed-Class-Space.init, value=0
SampleDataLoaderIT - type=GAUGE, name=pools.Compressed-Class-Space.max, value=1073741824
SampleDataLoaderIT - type=GAUGE, name=pools.Compressed-Class-Space.usage, value=0.003872111439704895
SampleDataLoaderIT - type=GAUGE, name=pools.Compressed-Class-Space.used, value=4157648
SampleDataLoaderIT - type=GAUGE, name=pools.Metaspace.committed, value=36872192
SampleDataLoaderIT - type=GAUGE, name=pools.Metaspace.init, value=0
SampleDataLoaderIT - type=GAUGE, name=pools.Metaspace.max, value=-1
SampleDataLoaderIT - type=GAUGE, name=pools.Metaspace.usage, value=0.966329964868918
SampleDataLoaderIT - type=GAUGE, name=pools.Metaspace.used, value=35630704
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Eden-Space.committed, value=1820327936
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Eden-Space.init, value=132120576
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Eden-Space.max, value=1863319552
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Eden-Space.usage, value=0.7033871386114238
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Eden-Space.used, value=1310635008
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Old-Gen.committed, value=1524629504
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Old-Gen.init, value=351272960
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Old-Gen.max, value=5603590144
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Old-Gen.usage, value=0.03405336562744871
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Old-Gen.used, value=190821104
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Survivor-Space.committed, value=447741952
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Survivor-Space.init, value=21495808
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Survivor-Space.max, value=447741952
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Survivor-Space.usage, value=4.3911007025761124E-4
SampleDataLoaderIT - type=GAUGE, name=pools.PS-Survivor-Space.used, value=196608
SampleDataLoaderIT - type=GAUGE, name=total.committed, value=3851984896
SampleDataLoaderIT - type=GAUGE, name=total.init, value=528941056
SampleDataLoaderIT - type=GAUGE, name=total.max, value=7471628287
SampleDataLoaderIT - type=GAUGE, name=total.used, value=1558045088
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.beneficiaries.files, count=3, min=853.4077639999999, max=1070.644272, mean=979.8369298842584, stddev=92.8424795792039, median=1018.7214309999999, p75=1070.644272, p95=1070.644272, p98=1070.644272, p99=1070.644272, p999=1070.644272, mean_rate=0.0037509295727710377, m1=1.1479965211453903E-6, m5=0.043103160413587985, m15=0.24942340583982575, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.beneficiaries.records, count=343644, min=0.003476, max=0.133662, mean=0.004745917437124193, stddev=0.0042005453672210235, median=0.004532, p75=0.004775, p95=0.0056689999999999996, p98=0.008017999999999999, p99=0.008541, p999=0.034838999999999995, mean_rate=429.66095174183977, m1=0.13150070550416207, m5=4937.380819055678, m15=28570.952292140344, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.carrier.files, count=2, min=344331.454721, max=414478.881949, mean=414338.2217299871, stddev=3138.019627068616, median=414478.881949, p75=414478.881949, p95=414478.881949, p98=414478.881949, p99=414478.881949, p999=414478.881949, mean_rate=0.0025607701525886986, m1=0.01146949255646413, m5=0.003867935248225862, m15=0.0017670282509835975, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.carrier.records, count=4741335, min=0.107858, max=285.922172, mean=0.5568188100447413, stddev=11.285454146462277, median=0.10946, p75=0.110181, p95=0.11451, p98=0.123389, p99=0.133707, p999=285.922172, mean_rate=6070.727080980261, m1=3419.2818641996278, m5=5426.737720401213, m15=6182.242395405783, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.drug.files, count=1, min=22198.594074999997, max=22198.594074999997, mean=22198.594074999997, stddev=0.0, median=22198.594074999997, p75=22198.594074999997, p95=22198.594074999997, p98=22198.594074999997, p99=22198.594074999997, p999=22198.594074999997, mean_rate=0.04503323769931169, m1=0.0, m5=0.0, m15=0.0, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.drug.records, count=5131364, min=0.001462, max=0.010695, mean=0.0018315648796222593, stddev=4.942911512857224E-4, median=0.001691, p75=0.0017679999999999998, p95=0.00294, p98=0.0031869999999999997, p99=0.003391, p999=0.005693, mean_rate=231073.40044333317, m1=185256.582670748, m5=171167.67394548384, m15=168575.4550726358, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.inpatient.files, count=1, min=1387.519333, max=1387.519333, mean=1387.519333, stddev=0.0, median=1387.519333, p75=1387.519333, p95=1387.519333, p98=1387.519333, p99=1387.519333, p999=1387.519333, mean_rate=0.0012549206910813366, m1=3.826655070484632E-7, m5=0.014367720137862668, m15=0.08314113527994192, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.inpatient.records, count=66705, min=0.008667, max=0.136539, mean=0.010269256887051223, stddev=0.004156764105707119, median=0.00998, p75=0.010197, p95=0.011092999999999999, p98=0.016783, p99=0.018206, p999=0.019927, mean_rate=83.70937776915967, m1=0.02552570264766775, m5=958.3987717961285, m15=5545.929428848522, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.outpatient.files, count=1, min=14457.402933, max=14457.402933, mean=14457.402933, stddev=0.0, median=14457.402933, p75=14457.402933, p95=14457.402933, p98=14457.402933, p99=14457.402933, p999=14457.402933, mean_rate=0.0012571077748900715, m1=3.614519217314528E-8, m5=2.455268534698646E-4, m15=4.657610042756379E-4, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.outpatient.records, count=790790, min=0.00547, max=0.042609, mean=0.008825146158953277, stddev=0.003124639830129827, median=0.007659, p75=0.00811, p95=0.015403, p98=0.018165, p99=0.019606, p999=0.042609, mean_rate=994.1070262574154, m1=0.13706006951137611, m5=4635.536996146234, m15=26373.22807296271, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.samples, count=1, min=799798.761712, max=799798.761712, mean=799798.761712, stddev=0.0, median=799798.761712, p75=799798.761712, p95=799798.761712, p98=799798.761712, p99=799798.761712, p999=799798.761712, mean_rate=0.0012502986663473481, m1=0.0, m5=0.0, m15=0.0, rate_unit=events/second, duration_unit=milliseconds
SampleDataLoaderIT - type=TIMER, name=gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader.samples.tx, count=1, min=1.141328, max=1.141328, mean=1.141328, stddev=0.0, median=1.141328, p75=1.141328, p95=1.141328, p98=1.141328, p99=1.141328, p999=1.141328, mean_rate=86.42333485351979, m1=0.0, m5=0.0, m15=0.0, rate_unit=events/second, duration_unit=milliseconds
```