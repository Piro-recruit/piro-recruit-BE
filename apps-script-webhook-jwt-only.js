// ==========================================
// 피로그래밍 구글 폼 웹훅 연동 - JWT 전용 버전
// ==========================================

// 설정값을 저장하는 함수 (최초 한 번만 실행)
function saveConfiguration() {
  var properties = PropertiesService.getScriptProperties();

  properties.setProperties({
    'FORM_ID': '1AepQVswMu83SX-NdnTHH-vng9dp3wND1AB5ENlENzAM',
    'WEBHOOK_URL': 'https://api.piro-recruit.kro.kr/api/webhook/applications/receive',
    'TEST_URL': 'https://api.piro-recruit.kro.kr/api/webhook/applications/test',
    'JWT_EXCHANGE_URL': 'https://api.piro-recruit.kro.kr/api/admin/token/exchange',
    'SPREADSHEET_ID': '19CjgpaA2p8QVyZ_ukAYVooqLYO2kpZ_FQ8uGjjNP3gg',
    'API_KEY': 'piro-recruit-webhook-2025'
  });

  console.log('설정값이 저장되었습니다.');
}

// 설정값을 불러오는 함수
function getConfig() {
  var properties = PropertiesService.getScriptProperties();
  var config = {
    FORM_ID: properties.getProperty('FORM_ID'),
    WEBHOOK_URL: properties.getProperty('WEBHOOK_URL'),
    TEST_URL: properties.getProperty('TEST_URL'),
    JWT_EXCHANGE_URL: properties.getProperty('JWT_EXCHANGE_URL'),
    SPREADSHEET_ID: properties.getProperty('SPREADSHEET_ID'),
    API_KEY: properties.getProperty('API_KEY')
  };

  // 필수 설정값 확인
  if (!config.API_KEY || !config.JWT_EXCHANGE_URL) {
    console.warn('설정값이 없습니다. saveConfiguration() 함수를 먼저 실행하세요.');
  }

  return config;
}

// ==========================================
// JWT 토큰 관리 함수들
// ==========================================

// JWT 토큰 발급/갱신
function getJwtToken(forceRefresh = false) {
  try {
    var properties = PropertiesService.getScriptProperties();
    var storedToken = properties.getProperty('JWT_TOKEN');
    var tokenExpiry = properties.getProperty('JWT_TOKEN_EXPIRY');
    var now = new Date().getTime();

    // 기존 토큰이 있고 아직 유효하면 재사용 (5분 여유)
    if (!forceRefresh && storedToken && tokenExpiry) {
      var expiryTime = parseInt(tokenExpiry);
      var bufferTime = 5 * 60 * 1000; // 5분 버퍼

      if (now < (expiryTime - bufferTime)) {
        console.log('기존 JWT 토큰 재사용');
        return storedToken;
      }
    }

    console.log('새로운 JWT 토큰 발급 시작...');

    var CONFIG = getConfig();
    if (!CONFIG.API_KEY || !CONFIG.JWT_EXCHANGE_URL) {
      throw new Error('API Key 또는 JWT Exchange URL이 설정되지 않았습니다.');
    }

    // JWT 토큰 발급 요청
    var payload = {
      apiKey: CONFIG.API_KEY,
      purpose: 'webhook'
    };

    var options = {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    };

    var response = UrlFetchApp.fetch(CONFIG.JWT_EXCHANGE_URL, options);
    var responseCode = response.getResponseCode();
    var responseText = response.getContentText();

    if (responseCode !== 200) {
      console.error('JWT 발급 실패 (' + responseCode + '):', responseText);
      throw new Error('JWT 토큰 발급 실패: ' + responseText);
    }

    var result = JSON.parse(responseText);
    var tokenData = result.accessToken ? result : result.data;

    if (!tokenData || !tokenData.accessToken) {
      throw new Error('유효하지 않은 JWT 응답 형식');
    }

    // 토큰과 만료 시간 저장
    var expiryTime = now + (tokenData.expiresIn * 1000);

    properties.setProperties({
      'JWT_TOKEN': tokenData.accessToken,
      'JWT_TOKEN_EXPIRY': expiryTime.toString()
    });

    console.log('JWT 토큰 발급 성공 (만료: ' + new Date(expiryTime).toString() + ')');

    return tokenData.accessToken;

  } catch (error) {
    console.error('JWT 토큰 발급/갱신 실패:', error);
    logError('getJwtToken', error);
    throw error;
  }
}

// JWT 토큰 강제 갱신
function refreshJwtToken() {
  console.log('JWT 토큰 강제 갱신...');
  return getJwtToken(true);
}

// 저장된 JWT 토큰 정보 확인
function viewJwtTokenInfo() {
  var properties = PropertiesService.getScriptProperties();
  var token = properties.getProperty('JWT_TOKEN');
  var expiry = properties.getProperty('JWT_TOKEN_EXPIRY');

  console.log('🔑 JWT 토큰 정보:');

  if (token) {
    console.log('- 토큰: ' + token.substring(0, 20) + '...');

    if (expiry) {
      var expiryDate = new Date(parseInt(expiry));
      var now = new Date();
      var isExpired = now.getTime() >= parseInt(expiry);

      console.log('- 만료 시간: ' + expiryDate.toString());
      console.log('- 현재 시간: ' + now.toString());
      console.log('- 상태: ' + (isExpired ? '만료됨' : '유효함'));

      if (!isExpired) {
        var remainingMinutes = Math.floor((parseInt(expiry) - now.getTime()) / (1000 * 60));
        console.log('- 남은 시간: ' + remainingMinutes + '분');
      }
    } else {
      console.log('- 만료 시간: 정보 없음');
    }
  } else {
    console.log('- 저장된 토큰이 없습니다.');
  }
}

// JWT 토큰 삭제
function clearJwtToken() {
  var properties = PropertiesService.getScriptProperties();
  properties.deleteProperty('JWT_TOKEN');
  properties.deleteProperty('JWT_TOKEN_EXPIRY');
  console.log('JWT 토큰이 삭제되었습니다.');
}

// ==========================================
// 설정 관리 함수들
// ==========================================

// 웹훅 URL 변경
function updateWebhookUrl(newUrl) {
  if (!newUrl) {
    console.error('URL을 입력해주세요.');
    return;
  }

  PropertiesService.getScriptProperties().setProperties({
    'WEBHOOK_URL': newUrl,
    'TEST_URL': newUrl.replace('/receive', '/test')
  });

  console.log('웹훅 URL이 업데이트되었습니다.');
  console.log('- WEBHOOK_URL: ' + newUrl);
  console.log('- TEST_URL: ' + newUrl.replace('/receive', '/test'));
}

// JWT Exchange URL 변경
function updateJwtExchangeUrl(newUrl) {
  if (!newUrl) {
    console.error('URL을 입력해주세요.');
    return;
  }

  PropertiesService.getScriptProperties().setProperty('JWT_EXCHANGE_URL', newUrl);

  // 기존 JWT 토큰 삭제하여 새로 발급받도록 함
  clearJwtToken();

  console.log('JWT Exchange URL이 업데이트되었습니다.');
  console.log('JWT 토큰이 초기화되었습니다.');
}

// API Key 변경 (JWT 토큰도 자동 갱신)
function updateApiKey(newApiKey) {
  if (!newApiKey) {
    console.error('API Key를 입력해주세요.');
    return;
  }

  PropertiesService.getScriptProperties().setProperty('API_KEY', newApiKey);

  // 기존 JWT 토큰 삭제하여 새로 발급받도록 함
  clearJwtToken();

  console.log('API Key가 업데이트되었습니다. JWT 토큰이 초기화되었습니다.');
}

// 현재 설정값 확인
function viewCurrentConfig() {
  var config = getConfig();
  console.log('📋 현재 설정값:');
  console.log('- FORM_ID:', config.FORM_ID);
  console.log('- WEBHOOK_URL:', config.WEBHOOK_URL);
  console.log('- TEST_URL:', config.TEST_URL);
  console.log('- JWT_EXCHANGE_URL:', config.JWT_EXCHANGE_URL);
  console.log('- SPREADSHEET_ID:', config.SPREADSHEET_ID);
  console.log('- API_KEY:', config.API_KEY ? '설정됨 (' + config.API_KEY.substring(0, 10) + '...)' : '설정 안됨');
  return config;
}

// 설정값 초기화 (주의!)
function resetConfiguration() {
  PropertiesService.getScriptProperties().deleteAll();
  console.log('⚠️ 모든 설정값이 초기화되었습니다.');
  console.log('saveConfiguration() 함수를 다시 실행하세요.');
}

// ==========================================
// 폼 제출 처리 (메인 함수)
// ==========================================

// 폼 제출 시 자동 실행되는 함수
function onFormSubmit(e) {
  try {
    var CONFIG = getConfig();
    if (!CONFIG.API_KEY) {
      console.error('설정값이 없습니다. saveConfiguration() 함수를 먼저 실행하세요.');
      return;
    }

    console.log('📝 폼 제출 이벤트 발생');

    var formResponse = e.response;
    var itemResponses = formResponse.getItemResponses();
    var timestamp = formResponse.getTimestamp();
    var email = formResponse.getRespondentEmail();

    // 응답 데이터를 객체로 변환 (순서 보장)
    var formData = {};
    var formDataOrder = []; // 질문 순서 저장

    for (var i = 0; i < itemResponses.length; i++) {
      var itemResponse = itemResponses[i];
      var question = itemResponse.getItem().getTitle();
      var answer = itemResponse.getResponse();

      // 빈 값 처리
      if (answer !== null && answer !== undefined && answer !== '') {
        formData[question] = answer;
        formDataOrder.push(question); // 순서 정보 저장
      }
    }

    // 지원자 정보 추출
    var applicantName = formData['이름'] || formData['성명'] || '';
    var applicantEmail = email || formData['이메일 주소'] || formData['이메일'] || '';

    // 추가 필드 추출 (백엔드 DTO와 매칭)
    var school = formData['대학교'] || formData['학교'] || '';
    var department = formData['학과'] || formData['전공학과'] || '';
    var grade = formData['학년'] || '';
    var major = formData['전공 여부'] || formData['전공'] || '';
    var phoneNumber = formData['전화번호'] || formData['휴대폰 번호'] || '';

    console.log('👤 지원자:', applicantName, '(' + applicantEmail + ')');
    console.log('🏫 학교정보:', school, department, grade, major);

    // 서버 전송용 데이터 구성 (백엔드 DTO 구조에 맞춤 + 순서 정보 포함)
    var webhookPayload = {
      formId: CONFIG.FORM_ID,
      applicantName: applicantName,
      applicantEmail: applicantEmail,
      formResponseId: 'response_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9),
      submissionTimestamp: timestamp.toISOString(),
      school: school,
      department: department,
      grade: grade,
      major: major,
      phoneNumber: phoneNumber,
      formData: formData,
      formDataOrder: formDataOrder // 📝 질문 순서 정보 추가
    };

    console.log('📦 웹훅 페이로드 준비 완료');

    // JWT 토큰으로 서버에 데이터 전송
    var result = sendToWebhook(webhookPayload, CONFIG.WEBHOOK_URL);

    if (result.success) {
      console.log('✅ 지원서 전송 성공!');
      logSuccess('onFormSubmit', '지원자: ' + applicantName + ' (' + applicantEmail + ')');
    } else {
      console.error('❌ 지원서 전송 실패:', result.error || 'Unknown error');
      logError('onFormSubmit', result.error || 'Unknown error');
    }

  } catch (error) {
    console.error('💥 폼 제출 처리 중 오류:', error);
    logError('onFormSubmit', error);
  }
}

// ==========================================
// 웹훅 전송 함수 (JWT 전용)
// ==========================================

// JWT를 사용해 웹훅으로 데이터 전송
function sendToWebhook(payload, url, retryCount = 0) {
  try {
    var maxRetries = 2;

    // JWT 토큰 발급/갱신
    var jwtToken = getJwtToken();

    var options = {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + jwtToken
      },
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    };

    console.log('🔐 JWT 인증으로 서버 요청:', url);

    var response = UrlFetchApp.fetch(url, options);
    var responseCode = response.getResponseCode();
    var responseText = response.getContentText();

    console.log('📨 서버 응답 코드:', responseCode);

    if (responseCode >= 200 && responseCode < 300) {
      console.log('✅ 전송 성공:', responseText);
      return {
        success: true,
        responseCode: responseCode,
        responseText: responseText
      };
    }
    // JWT 토큰이 만료되었거나 인증 오류인 경우 재시도
    else if (responseCode === 401 && retryCount < maxRetries) {
      console.log('🔄 JWT 토큰 만료 또는 인증 오류. 토큰 갱신 후 재시도... (' + (retryCount + 1) + '/' + maxRetries + ')');

      // 토큰 강제 갱신
      getJwtToken(true);

      // 재귀 호출로 재시도
      return sendToWebhook(payload, url, retryCount + 1);
    }
    else {
      console.error('❌ 전송 실패 (' + responseCode + '):', responseText);
      return {
        success: false,
        responseCode: responseCode,
        responseText: responseText,
        error: 'HTTP ' + responseCode + ': ' + responseText
      };
    }

  } catch (error) {
    console.error('🌐 네트워크 오류:', error.toString());
    return {
      success: false,
      error: 'Network Error: ' + error.toString()
    };
  }
}

// ==========================================
// 테스트 함수들
// ==========================================

// JWT 토큰 발급 테스트
function testJwtTokenGeneration() {
  console.log('🧪 JWT 토큰 발급 테스트 시작...');

  try {
    var token = getJwtToken(true); // 강제 갱신
    console.log('✅ JWT 토큰 발급 테스트 성공!');
    console.log('토큰: ' + token.substring(0, 30) + '...');

    viewJwtTokenInfo();

    return { success: true, token: token };
  } catch (error) {
    console.error('❌ JWT 토큰 발급 테스트 실패:', error);
    return { success: false, error: error.toString() };
  }
}

// 웹훅 연결 테스트
function testWebhookConnection() {
  console.log('🧪 웹훅 연결 테스트 시작...');

  var CONFIG = getConfig();
  if (!CONFIG.API_KEY) {
    console.error('❌ 설정값이 없습니다. saveConfiguration() 함수를 먼저 실행하세요.');
    return { success: false, error: '설정값 없음' };
  }

  var testPayload = {
    message: 'Apps Script JWT 연결 테스트',
    timestamp: new Date().toISOString(),
    formId: CONFIG.FORM_ID,
    testData: {
      scriptVersion: 'JWT Only v1.0',
      testTime: Utilities.formatDate(new Date(), Session.getScriptTimeZone(), 'yyyy-MM-dd HH:mm:ss')
    }
  };

  var result = sendToWebhook(testPayload, CONFIG.TEST_URL);

  if (result.success) {
    console.log('✅ 테스트 성공! JWT 인증된 서버 연결이 정상입니다.');
    logSuccess('testWebhookConnection', 'JWT 테스트 성공');
  } else {
    console.error('❌ 테스트 실패:', result.error);
    logError('testWebhookConnection', result.error);
  }

  return result;
}

// 실제 지원서 전송 테스트
function testRealApplicationSubmit() {
  console.log('🧪 실제 지원서 전송 테스트 시작...');

  var CONFIG = getConfig();
  if (!CONFIG.API_KEY) {
    console.error('❌ 설정값이 없습니다.');
    return { success: false, error: '설정값 없음' };
  }

  var testFormData = {
    '이름': '테스트 지원자',
    '이메일 주소': 'test.jwt.applicant@example.com',
    '대학교': '테스트 대학교',
    '학과': '컴퓨터공학과',
    '전공 여부': '주전공',
    '학년': '3학년',
    '전화번호': '010-1234-5678',
    '자기소개': 'JWT 인증 Apps Script 테스트용 자기소개서입니다.',
    '협업경험': 'JWT 테스트용 협업 경험입니다.',
    '겨울방학 계획': 'JWT 테스트용 겨울방학 계획입니다.',
    '코딩테스트': '완료'
  };

  var testPayload = {
    formId: CONFIG.FORM_ID,
    applicantName: '테스트 지원자',
    applicantEmail: 'test.jwt.applicant@example.com',
    formResponseId: 'test_jwt_' + Date.now(),
    submissionTimestamp: new Date().toISOString(),
    school: '테스트 대학교',
    department: '컴퓨터공학과',
    grade: '3학년',
    major: '주전공',
    phoneNumber: '010-1234-5678',
    formData: testFormData,
    formDataOrder: Object.keys(testFormData) // 📝 동적으로 순서 생성
  };

  var result = sendToWebhook(testPayload, CONFIG.WEBHOOK_URL);

  if (result.success) {
    console.log('✅ 실제 지원서 전송 테스트 성공!');
  } else {
    console.error('❌ 실제 지원서 전송 테스트 실패:', result.error);
  }

  return result;
}

// 전체 플로우 테스트
function testFullFlow() {
  console.log('🧪 === 전체 플로우 테스트 시작 ===');

  var results = [];

  // 1. 설정 확인
  console.log('1️⃣ 설정 확인...');
  viewCurrentConfig();

  // 2. JWT 토큰 발급
  console.log('2️⃣ JWT 토큰 발급 테스트...');
  var jwtResult = testJwtTokenGeneration();
  results.push({ step: 'JWT 발급', success: jwtResult.success });
  if (!jwtResult.success) {
    console.error('❌ JWT 토큰 발급 실패. 테스트 중단.');
    return { success: false, results: results };
  }

  // 3. 웹훅 연결 테스트
  console.log('3️⃣ 웹훅 연결 테스트...');
  var connectionResult = testWebhookConnection();
  results.push({ step: '웹훅 연결', success: connectionResult.success });
  if (!connectionResult.success) {
    console.error('❌ 웹훅 연결 실패. 테스트 중단.');
    return { success: false, results: results };
  }

  var allSuccess = results.every(function(result) { return result.success; });

  if (allSuccess) {
    console.log('🎉 === 기본 연결 테스트 성공! ===');
  } else {
    console.log('⚠️ === 일부 테스트 실패 ===');
    results.forEach(function(result) {
      console.log('- ' + result.step + ': ' + (result.success ? '✅' : '❌'));
    });
  }

  return { success: allSuccess, results: results };
}

// ==========================================
// 로깅 함수들
// ==========================================

// 성공 로그 기록
function logSuccess(functionName, message) {
  var sheet = getLogSheet();
  if (sheet) {
    sheet.appendRow([
      new Date(),
      'SUCCESS',
      functionName,
      message || '',
      ''
    ]);
  }
}

// 에러 로그 기록
function logError(functionName, error) {
  var sheet = getLogSheet();
  if (sheet) {
    sheet.appendRow([
      new Date(),
      'ERROR',
      functionName,
      error ? error.toString() : 'Unknown error',
      error && error.stack ? error.stack : ''
    ]);
  }
}

// 로그 시트 가져오기/생성
function getLogSheet() {
  try {
    var CONFIG = getConfig();
    if (!CONFIG.SPREADSHEET_ID) {
      console.warn('⚠️ 스프레드시트 ID가 설정되지 않았습니다.');
      return null;
    }

    var spreadsheet = SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID);
    var logSheet = spreadsheet.getSheetByName('웹훅 로그');

    if (!logSheet) {
      console.log('📊 로그 시트를 생성합니다...');
      logSheet = spreadsheet.insertSheet('웹훅 로그');

      var headers = ['시간', '상태', '함수', '메시지', '스택 트레이스'];
      logSheet.getRange(1, 1, 1, headers.length).setValues([headers]);
      logSheet.getRange(1, 1, 1, headers.length).setFontWeight('bold');
      logSheet.autoResizeColumns(1, headers.length);

      console.log('✅ 로그 시트가 생성되었습니다.');
    }

    return logSheet;
  } catch (error) {
    console.error('💥 로그 시트 접근 실패:', error);
    return null;
  }
}

// 로그 시트 정리
function cleanupLogs() {
  try {
    var sheet = getLogSheet();
    if (!sheet) return;

    var lastRow = sheet.getLastRow();
    if (lastRow <= 100) {
      console.log('📊 로그가 100개 이하입니다. 정리하지 않습니다.');
      return;
    }

    sheet.deleteRows(2, 50);
    console.log('🧹 오래된 로그 50개를 삭제했습니다.');

  } catch (error) {
    console.error('💥 로그 정리 실패:', error);
  }
}

// ==========================================
// 유틸리티 함수들
// ==========================================

// 모든 트리거 확인
function listAllTriggers() {
  var triggers = ScriptApp.getProjectTriggers();
  console.log('🔧 현재 설정된 트리거 목록:');

  if (triggers.length === 0) {
    console.log('- 설정된 트리거가 없습니다.');
    console.log('💡 트리거 설정이 필요합니다: 편집 > 현재 프로젝트의 트리거');
  } else {
    for (var i = 0; i < triggers.length; i++) {
      var trigger = triggers[i];
      console.log('- ' + trigger.getHandlerFunction() + ' (' + trigger.getTriggerSource() + ')');
    }
  }
}

// 헬프 함수
function help() {
  console.log('📖 피로그래밍 구글 폼 웹훅 연동 도움말 (JWT 전용 버전)');
  console.log('');
  console.log('🔧 설정 함수:');
  console.log('  - saveConfiguration(): 최초 설정 (한 번만 실행)');
  console.log('  - viewCurrentConfig(): 현재 설정값 확인');
  console.log('  - updateApiKey(newKey): API Key 변경');
  console.log('  - updateWebhookUrl(newUrl): 웹훅 URL 변경');
  console.log('  - updateJwtExchangeUrl(newUrl): JWT Exchange URL 변경');
  console.log('  - resetConfiguration(): 모든 설정 초기화');
  console.log('');
  console.log('🔑 JWT 토큰 관리:');
  console.log('  - getJwtToken(): JWT 토큰 발급/갱신');
  console.log('  - refreshJwtToken(): JWT 토큰 강제 갱신');
  console.log('  - viewJwtTokenInfo(): JWT 토큰 정보 확인');
  console.log('  - clearJwtToken(): JWT 토큰 삭제');
  console.log('');
  console.log('🧪 테스트 함수:');
  console.log('  - testJwtTokenGeneration(): JWT 토큰 발급 테스트');
  console.log('  - testWebhookConnection(): 웹훅 연결 테스트');
  console.log('  - testRealApplicationSubmit(): 지원서 전송 테스트');
  console.log('  - testFullFlow(): 전체 플로우 테스트');
  console.log('');
  console.log('📊 로그 관리:');
  console.log('  - cleanupLogs(): 오래된 로그 정리');
  console.log('');
  console.log('🔧 유틸리티:');
  console.log('  - listAllTriggers(): 트리거 목록 확인');
  console.log('  - help(): 이 도움말 보기');
  console.log('');
  console.log('🚀 사용법:');
  console.log('  1. saveConfiguration() 실행');
  console.log('  2. testFullFlow() 로 연결 테스트');
  console.log('  3. 구글 폼에 트리거 설정 (onFormSubmit)');
}

// 빠른 시작 가이드
function quickStart() {
  console.log('🚀 빠른 시작 가이드');
  console.log('');
  console.log('1️⃣ 설정 저장:');
  console.log('   saveConfiguration()');
  console.log('');
  console.log('2️⃣ 설정 확인:');
  console.log('   viewCurrentConfig()');
  console.log('');
  console.log('3️⃣ 연결 테스트:');
  console.log('   testFullFlow()');
  console.log('');
  console.log('4️⃣ 트리거 설정:');
  console.log('   편집 > 현재 프로젝트의 트리거 > 트리거 추가');
  console.log('   - 함수: onFormSubmit');
  console.log('   - 이벤트 소스: 양식에서');
  console.log('   - 이벤트 유형: 양식 제출 시');
  console.log('');
  console.log('✅ 완료! 이제 구글 폼 제출 시 자동으로 웹훅이 전송됩니다.');
}