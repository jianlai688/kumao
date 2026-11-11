import request from '@/api/request'

const riskWords = ['诈骗', '赌博', '色情', '毒品', '枪支', '代考', '刷单', '加微信', '加V', '政治谣言', '裸聊', '返利', '贷款']

function localReview(item, category = '') {
  const text = [item.title, item.content, item.intro, item.reason, item.name, item.companyName].filter(Boolean).join(' ')
  const hits = riskWords.filter((word) => text.toLowerCase().includes(word.toLowerCase()))
  let risk = 8 + hits.length * 20
  if ((category === 'COMMENT' || category === 'DANMAKU') && /https?:\/\/|www\.|加我|1[3-9]\d{9}/i.test(text)) risk += 18
  if ((category === 'SONG' || category === 'MV') && !item.metadata?.mediaUrl) risk += 18
  risk = Math.min(0.98, risk / 100)
  return {
    decision: risk >= 0.7 ? 'REJECT' : risk >= 0.35 ? 'REVIEW' : 'PASS',
    riskScore: Math.round(risk * 100),
    reasons: hits.length ? `前端兜底规则命中风险词：${hits.join('、')}` : 'AI 服务暂不可达，前端仅做了基础风险词检查，请人工复核',
    signals: hits,
    source: 'LOCAL_RULE',
    model: 'frontend-fallback',
    fallbackReason: 'AI 服务暂不可达'
  }
}

/**
 * AI 审核建议：模型由管理员在「AI 模型配置」中指定，后台只返回建议。
 * 仅当 review-service 不可达时才使用前端兜底规则，并明确标注 LOCAL_RULE。
 */
export async function reviewWithAi(item, category) {
  const payload = {
    category,
    itemId: item.id == null ? null : String(item.id),
    title: item.title || item.name || item.companyName || '',
    content: item.content || item.intro || item.reason || '',
    metadata: item.metadata || {}
  }
  try {
    const res = await request.post('/admin/ai/review', payload, { silent: true })
    if (res?.data?.decision) return { ...res.data, source: res.data.source || 'AI_MODEL' }
  } catch (_) {
    // 后端不可用时降级，不阻断管理员处理队列。
  }
  return localReview(item, category)
}
