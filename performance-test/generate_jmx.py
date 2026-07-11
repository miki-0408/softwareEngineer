#!/usr/bin/env python3
"""生成 JMeter .jmx — 纯性能测试，无 assertion，响应时间 + 吞吐量"""
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, 'data').replace('\\', '/')

def tg(name, threads, rampup, loops, *entries):
    """
    entries: alternating (element_xml, [child_xml, ...])
    child_xml are placed in the hashTree after the element, each followed by <hashTree/>
    """
    inner = ''
    for i in range(0, len(entries), 2):
        elem = entries[i]
        kids = entries[i + 1] if i + 1 < len(entries) else []
        inner += elem + '\n'
        if kids:
            sub = ''
            for k in kids:
                sub += k + '\n<hashTree/>\n'
            inner += '<hashTree>\n' + sub + '</hashTree>\n'
        else:
            inner += '<hashTree/>\n'
    return f'''<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="{name}">
<intProp name="ThreadGroup.num_threads">{threads}</intProp>
<intProp name="ThreadGroup.ramp_time">{rampup}</intProp>
<stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
<elementProp name="ThreadGroup.main_controller" elementType="LoopController">
<intProp name="LoopController.loops">{loops}</intProp>
<boolProp name="LoopController.continue_forever">false</boolProp>
</elementProp>
</ThreadGroup>
<hashTree>
{inner}
</hashTree>'''

def sampler(name, path, params=None, method='POST', files=None):
    p = f'''<stringProp name="HTTPSampler.domain">${{BASE_URL}}</stringProp>
<stringProp name="HTTPSampler.port">8080</stringProp>
<stringProp name="HTTPSampler.protocol">http</stringProp>
<stringProp name="HTTPSampler.path">{path}</stringProp>
<stringProp name="HTTPSampler.method">{method}</stringProp>
<boolProp name="HTTPSampler.multipart_post">{"true" if files else "false"}</boolProp>
<boolProp name="HTTPSampler.postBodyRaw">false</boolProp>'''
    if params:
        p += '''\n<elementProp name="HTTPsampler.Arguments" elementType="Arguments">
<collectionProp name="Arguments.arguments">\n'''
        for k, v in params:
            p += f'''<elementProp name="{k}" elementType="HTTPArgument">
<stringProp name="Argument.name">{k}</stringProp>
<stringProp name="Argument.value">{v}</stringProp>
<boolProp name="HTTPArgument.always_encode">false</boolProp>
</elementProp>\n'''
        p += '</collectionProp>\n</elementProp>\n'
    if files:
        p += '<elementProp name="HTTPsampler.Files" elementType="HTTPFileArgs">\n<collectionProp name="HTTPFileArgs.files">\n'
        for f in files:
            p += f'''<elementProp name="" elementType="HTTPFileArg">
<stringProp name="File.path">{f["path"]}</stringProp>
<stringProp name="File.paramname">{f["param"]}</stringProp>
<stringProp name="File.mimetype">{f.get("mime","application/octet-stream")}</stringProp>
</elementProp>\n'''
        p += '</collectionProp>\n</elementProp>\n'
    return f'<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="{name}">{p}</HTTPSamplerProxy>'

def header(h):
    items = ''
    for k, v in h:
        items += f'<elementProp name="{k}" elementType="Header">\n<stringProp name="Header.name">{k}</stringProp>\n<stringProp name="Header.value">{v}</stringProp>\n</elementProp>\n'
    return f'<HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Headers">\n<collectionProp name="HeaderManager.headers">{items}</collectionProp>\n</HeaderManager>'

def reg_ext(name, regex, var, tmpl='$1$', match=1):
    return f'<RegexExtractor guiclass="RegexExtractorGui" testclass="RegexExtractor" testname="{name}">\n<stringProp name="RegexExtractor.regex">{regex}</stringProp>\n<stringProp name="RegexExtractor.template">{tmpl}</stringProp>\n<stringProp name="RegexExtractor.referenceName">{var}</stringProp>\n<stringProp name="RegexExtractor.match_number">{match}</stringProp>\n</RegexExtractor>'
    return f'<JSONPostProcessor guiclass="JSONPostProcessorGui" testclass="JSONPostProcessor" testname="{name}">\n<stringProp name="JSONPostProcessor.jsonExpr">{expr}</stringProp>\n<stringProp name="JSONPostProcessor.referenceNames">{var}</stringProp>\n<stringProp name="JSONPostProcessor.matchNumbers">1</stringProp>\n</JSONPostProcessor>'

def csv_cfg(filename, vars):
    return f'<CSVDataSet guiclass="TestBeanGUI" testclass="CSVDataSet" testname="CSV Data">\n<stringProp name="delimiter">,</stringProp>\n<stringProp name="fileEncoding">UTF-8</stringProp>\n<stringProp name="filename">{filename}</stringProp>\n<stringProp name="variableNames">{",".join(vars)}</stringProp>\n<boolProp name="quotedData">false</boolProp>\n<boolProp name="recycle">true</boolProp>\n<boolProp name="stopThread">false</boolProp>\n<stringProp name="shareMode">all</stringProp>\n</CSVDataSet>'

def collector(name, gc='SummaryReport'):
    return f'<ResultCollector guiclass="{gc}" testclass="ResultCollector" testname="{name}">\n<boolProp name="ResultCollector.success_only_logging">false</boolProp>\n<boolProp name="ResultCollector.error_logging">true</boolProp>\n</ResultCollector>'


def build_single():
    login_tg = tg('01-Login', 10, 10, 50,
        sampler('Login', '/login', [('username','perf_test_1'),('password','test123456')]))
    fl_tg = tg('02-FileList', 10, 10, 50,
        csv_cfg(DATA_DIR + '/test_users.csv', ['username','password']), [],
        sampler('Login', '/login', [('username','${username}'),('password','${password}')]),
            [json_ext('Extract Token', '$.data.token', 'TOKEN')],
        sampler('FileList', '/user/file/list', [('dirId','${__P(dirId,61)}')]),
            [header([('Authorization','Bearer ${TOKEN}')])])
    up_tg = tg('03-Upload', 3, 10, 10,
        csv_cfg(DATA_DIR + '/test_users.csv', ['username','password']), [],
        sampler('Login', '/login', [('username','${username}'),('password','${password}')]),
            [json_ext('Extract Token', '$.data.token', 'TOKEN')],
        sampler('Upload', '/user/file/upload',
            [('dirId','${__P(dirId,61)}'),('packMethod','none'),('compressMethod','lz77')],
            files=[{'path':DATA_DIR+'/sample.txt','param':'files','mime':'text/plain'}]),
            [header([('Authorization','Bearer ${TOKEN}')])])
    rc_tg = tg('04-RecycleBin', 10, 10, 30,
        csv_cfg(DATA_DIR + '/test_users.csv', ['username','password']), [],
        sampler('Login', '/login', [('username','${username}'),('password','${password}')]),
            [json_ext('Extract Token', '$.data.token', 'TOKEN')],
        sampler('RecycleList', '/user/recycle/list'),
            [header([('Authorization','Bearer ${TOKEN}')])])
    reg_tg = tg('05-Register', 5, 5, 10,
        csv_cfg(DATA_DIR + '/test_users.csv', ['username','password']), [],
        sampler('Register', '/register',
            [('username','jmeter_reg_${__threadNum}_${__time(YMDhms)}'),('password','test123456')]))

    all_tgs = '\n'.join([login_tg, fl_tg, up_tg, rc_tg, reg_tg])
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
<hashTree>
<TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Netdisk - Single API"/>
<hashTree>
<Arguments guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables">
<collectionProp name="Arguments.arguments">
<elementProp name="BASE_URL" elementType="Argument">
<stringProp name="Argument.name">BASE_URL</stringProp>
<stringProp name="Argument.value">localhost</stringProp>
</elementProp>
</collectionProp>
</Arguments>
<hashTree/>
{all_tgs}
</hashTree>
</hashTree>
</jmeterTestPlan>'''

def build_mixed():
    mixed = tg('Mixed-10u', 10, 30, 20,
        csv_cfg(DATA_DIR + '/test_users.csv', ['username','password']), [],
        sampler('Login', '/login', [('username','${username}'),('password','${password}')]),
            [json_ext('Extract Token', '$.data.token', 'TOKEN')],
        sampler('Browse', '/user/file/list', [('dirId','${__P(dirId,61)}')]),
            [header([('Authorization','Bearer ${TOKEN}')])],
        sampler('Recycle', '/user/recycle/list'), [header([('Authorization','Bearer ${TOKEN}')])],
        sampler('Download', '/user/file/download', [('fileId','${__Random(1,5)}')]),
            [header([('Authorization','Bearer ${TOKEN}')])],
        sampler('Upload', '/user/file/upload',
            [('dirId','${__P(dirId,61)}'),('packMethod','none'),('compressMethod','lz77')],
            files=[{'path':DATA_DIR+'/sample.txt','param':'files','mime':'text/plain'}]),
            [header([('Authorization','Bearer ${TOKEN}')])])
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
<hashTree>
<TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Netdisk - Mixed Scenario"/>
<hashTree>
<Arguments guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables">
<collectionProp name="Arguments.arguments">
<elementProp name="BASE_URL" elementType="Argument">
<stringProp name="Argument.name">BASE_URL</stringProp>
<stringProp name="Argument.value">localhost</stringProp>
</elementProp>
</collectionProp>
</Arguments>
<hashTree/>
{mixed}
</hashTree>
</hashTree>
</jmeterTestPlan>'''

if __name__ == '__main__':
    for name, fn in [('single-api-test.jmx', build_single), ('mixed-scenario-test.jmx', build_mixed)]:
        p = os.path.join(BASE_DIR, name)
        with open(p, 'w', encoding='utf-8') as f:
            f.write(fn())
        print(f'Created: {name}')
