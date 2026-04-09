pub struct AIHeuristic;
impl AIHeuristic {
    pub fn new() -> Self { AIHeuristic }
    pub fn analyze(&self, domain: &str) -> (bool, f32) {
        if domain.len() > 30 && domain.chars().filter(|c| c.is_numeric()).count() > 5 { return (true, 0.85); }
        (false, 0.0)
    }
}
